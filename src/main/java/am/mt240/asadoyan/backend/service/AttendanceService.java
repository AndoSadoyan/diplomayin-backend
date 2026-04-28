package am.mt240.asadoyan.backend.service;

import am.mt240.asadoyan.backend.dto.*;
import am.mt240.asadoyan.backend.model.*;
import am.mt240.asadoyan.backend.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import am.mt240.asadoyan.backend.model.CourseSchedule.ClassPeriod;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceSessionRepository sessionRepo;

    @Autowired
    private CourseScheduleRepository scheduleRepo;

    @Autowired
    private CourseRepository courseRepo;

    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private DailyAttendanceSummaryRepository summaryRepo;

    @Autowired
    private SemesterAttendanceStatsRepository statsRepo;

    @Transactional
    public CheckinResponse checkin(CheckinRequest request) {
        Instant timestamp = Instant.ofEpochMilli(request.getTimestamp());
        ZonedDateTime zdt = timestamp.atZone(ZoneId.systemDefault());
        ClassPeriod currentPeriod = ClassPeriod.getCurrentPeriod();

        Optional<CourseSchedule> scheduleOpt = scheduleRepo.findFirstByRoomIdAndDayOfWeekAndClassPeriod(
                request.getRoomId(),
                zdt.getDayOfWeek(),
                currentPeriod
        );

        if (scheduleOpt.isEmpty()) {
            throw new RuntimeException("No class scheduled in room " + request.getRoomId() + 
                " on " + currentPeriod + " period on " + zdt.getDayOfWeek());
        }

        CourseSchedule schedule = scheduleOpt.get();

        Student student = studentRepo.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found: " + request.getStudentId()));

        if (!schedule.getGroupName().equals(student.getGroup())) {
            throw new RuntimeException("Student " + request.getStudentId() + 
                " (group: " + student.getGroup() + ") does not belong to this class (group: " + 
                schedule.getGroupName() + ")");
        }

        if (schedule.getSubgroup() != null && !schedule.getSubgroup().equals(student.getSubgroup())) {
            throw new RuntimeException("Student " + request.getStudentId() + 
                " (subgroup: " + student.getSubgroup() + ") does not belong to this class (subgroup: " + 
                schedule.getSubgroup() + ")");
        }

        AttendanceSession session = new AttendanceSession();
        session.setStudentId(request.getStudentId());
        session.setRoomId(request.getRoomId());
        session.setEntryTime(timestamp);
        session.setLastSeen(timestamp);
        session.setStatus(AttendanceSession.SessionStatus.ACTIVE);
        session.setAvgConfidenceScore(request.getConfidenceScore());
        session.setCourseScheduleId(schedule.getId());

        session = sessionRepo.save(session);

        CheckinResponse response = new CheckinResponse();
        response.setSessionId(session.getId());
        response.setCourseId(schedule.getCourseId());

        courseRepo.findById(schedule.getCourseId()).ifPresent(course -> {
            response.setCourseName(course.getName());
            response.setCourseCode(course.getCode());
        });

        return response;
    }

    @Transactional
    public void heartbeat(HeartbeatRequest request) {
        AttendanceSession session = sessionRepo.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found: " + request.getSessionId()));

        Instant timestamp = Instant.ofEpochMilli(request.getTimestamp());
        session.setLastSeen(timestamp);

        long durationSeconds = Duration.between(session.getEntryTime(), timestamp).getSeconds();
        session.setTotalDurationSeconds((int) durationSeconds);

        if (request.getConfidenceScore() != null && session.getAvgConfidenceScore() != null) {
            float currentAvg = session.getAvgConfidenceScore();
            float newAvg = (currentAvg * 0.8f) + (request.getConfidenceScore() * 0.2f);
            session.setAvgConfidenceScore(newAvg);
        }

        sessionRepo.save(session);
    }

    @Transactional
    public Long checkout(CheckoutRequest request) {
        AttendanceSession session = sessionRepo.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found: " + request.getSessionId()));

        Instant exitTime = Instant.ofEpochMilli(request.getTimestamp());
        session.setExitTime(exitTime);
        session.setStatus(AttendanceSession.SessionStatus.COMPLETED);

        long durationSeconds = Duration.between(session.getEntryTime(), exitTime).getSeconds();
        session.setTotalDurationSeconds((int) durationSeconds);

        sessionRepo.save(session);

        updateDailySummary(session);

        return durationSeconds;
    }

    private void updateDailySummary(AttendanceSession session) {
        LocalDate date = session.getEntryTime().atZone(ZoneId.systemDefault()).toLocalDate();
        ZonedDateTime entryZdt = session.getEntryTime().atZone(ZoneId.systemDefault());

        CourseSchedule schedule = scheduleRepo.findById(session.getCourseScheduleId())
                .orElseThrow(() -> new RuntimeException("Course schedule not found: " + session.getCourseScheduleId()));
        
        String courseId = schedule.getCourseId();

        DailyAttendanceSummary summary = summaryRepo
                .findByStudentIdAndCourseIdAndDate(
                        session.getStudentId(),
                        courseId,
                        date
                )
                .orElse(new DailyAttendanceSummary(
                        session.getStudentId(),
                        courseId,
                        date
                ));

        long expectedDuration = Duration.between(schedule.getStartTime(), schedule.getEndTime()).getSeconds();
        summary.setExpectedDurationSeconds((int) expectedDuration);

        boolean isFirstSession = summary.getId() == null;

        summary.setTotalDurationSeconds(
                summary.getTotalDurationSeconds() + session.getTotalDurationSeconds()
        );

        if (isFirstSession) {
            LocalTime arrivalTime = entryZdt.toLocalTime();
            LocalTime graceEnd = schedule.getStartTime().plusMinutes(15);
            summary.setWasLate(arrivalTime.isAfter(graceEnd));
        }

        double attendancePercentage = summary.getAttendancePercentage();

        if (attendancePercentage < 25.0) {
            summary.setStatus(DailyAttendanceSummary.AttendanceStatus.ABSENT);
            summary.setNotes("Present only " + String.format("%.1f", attendancePercentage) + "% of class time");
        } else if (summary.getWasLate()) {
            summary.setStatus(DailyAttendanceSummary.AttendanceStatus.LATE);
        } else {
            summary.setStatus(DailyAttendanceSummary.AttendanceStatus.PRESENT);
        }

        summaryRepo.save(summary);

        updateSemesterStats(session.getStudentId(), courseId);
    }

    private void updateSemesterStats(String studentId, String courseId) {
        Course course = courseRepo.findById(courseId).orElseThrow();
        String semester = course.getSemester() != null ? course.getSemester() : currentSemester();

        LocalDate semStart = semesterStart(semester);
        LocalDate semEnd   = semesterEnd(semester);
        long totalClasses = scheduleRepo.findByCourseId(courseId).stream()
                .mapToLong(s -> countDayOccurrences(s.getDayOfWeek(), semStart, semEnd))
                .sum();

        List<DailyAttendanceSummary> summaries = summaryRepo.findByStudentIdAndCourseId(studentId, courseId);
        long excusedClasses = summaries.stream()
                .filter(s -> s.getStatus() == DailyAttendanceSummary.AttendanceStatus.EXCUSED)
                .count();
        long attendedClasses = summaries.stream()
                .filter(s -> s.getStatus() == DailyAttendanceSummary.AttendanceStatus.PRESENT ||
                        s.getStatus() == DailyAttendanceSummary.AttendanceStatus.LATE)
                .count();
        long lateArrivals = summaries.stream()
                .filter(s -> s.getStatus() == DailyAttendanceSummary.AttendanceStatus.LATE)
                .count();

        long effectiveTotal = Math.max(0, totalClasses - excusedClasses);
        double percentage = effectiveTotal > 0 ? (attendedClasses * 100.0 / effectiveTotal) : 0.0;

        SemesterAttendanceStats stats = statsRepo
                .findByStudentIdAndCourseIdAndSemester(studentId, courseId, semester)
                .orElse(new SemesterAttendanceStats(studentId, courseId, semester));

        stats.setTotalClasses((int) effectiveTotal);
        stats.setAttendedClasses((int) attendedClasses);
        stats.setLateArrivals((int) lateArrivals);
        stats.setAttendancePercentage(percentage);

        statsRepo.save(stats);
    }

    @Transactional
    public void excuseCourse(String courseId, LocalDate date) {
        List<CourseSchedule> schedules = scheduleRepo.findByCourseId(courseId).stream()
                .filter(s -> s.getDayOfWeek() == date.getDayOfWeek())
                .toList();

        if (schedules.isEmpty()) throw new RuntimeException("No schedule for this course on " + date.getDayOfWeek());

        for (CourseSchedule schedule : schedules) {
            List<Student> students = studentRepo.findAllFaceEmbeddingsByGroup(schedule.getGroupName()).stream()
                    .filter(s -> schedule.getSubgroup() == null || schedule.getSubgroup().equals(s.getSubgroup()))
                    .toList();

            for (Student student : students) {
                DailyAttendanceSummary summary = summaryRepo
                        .findByStudentIdAndCourseIdAndDate(student.getId(), courseId, date)
                        .orElse(new DailyAttendanceSummary(student.getId(), courseId, date));
                summary.setStatus(DailyAttendanceSummary.AttendanceStatus.EXCUSED);
                summaryRepo.save(summary);
                updateSemesterStats(student.getId(), courseId);
            }
        }
    }

    @Transactional
    public void excuseStudent(String studentId, String courseId, LocalDate startDate, LocalDate endDate) {
        List<DailyAttendanceSummary> toExcuse = summaryRepo.findByStudentIdAndCourseId(studentId, courseId)
                .stream()
                .filter(s -> !s.getDate().isBefore(startDate) && !s.getDate().isAfter(endDate))
                .filter(s -> s.getStatus() == DailyAttendanceSummary.AttendanceStatus.ABSENT)
                .toList();

        toExcuse.forEach(s -> s.setStatus(DailyAttendanceSummary.AttendanceStatus.EXCUSED));
        summaryRepo.saveAll(toExcuse);

        if (!toExcuse.isEmpty()) updateSemesterStats(studentId, courseId);
    }

    public List<ActivePresenceDTO> getActivePresence(String roomId) {
        List<AttendanceSession> activeSessions = sessionRepo.findByRoomIdAndStatus(
                roomId, AttendanceSession.SessionStatus.ACTIVE);
        return convertSessions(activeSessions);
    }

    public Map<String, List<ActivePresenceDTO>> getAllActivePresence() {
        List<AttendanceSession> activeSessions = sessionRepo.findByStatus(
                AttendanceSession.SessionStatus.ACTIVE);

        Map<String, String> sessionToRoom = activeSessions.stream()
                .collect(Collectors.toMap(AttendanceSession::getId, AttendanceSession::getRoomId));

        return convertSessions(activeSessions).stream()
                .collect(Collectors.groupingBy(dto ->
                        sessionToRoom.getOrDefault(dto.getSessionId(), "Unknown")));
    }

    private List<ActivePresenceDTO> convertSessions(List<AttendanceSession> sessions) {
        if (sessions.isEmpty()) return Collections.emptyList();

        Set<String> studentIds = sessions.stream().map(AttendanceSession::getStudentId).collect(Collectors.toSet());
        Set<String> scheduleIds = sessions.stream().map(AttendanceSession::getCourseScheduleId).collect(Collectors.toSet());

        Map<String, Student> students = new HashMap<>();
        studentRepo.findAllById(studentIds).forEach(s -> students.put(s.getId(), s));

        Map<String, CourseSchedule> schedules = new HashMap<>();
        scheduleRepo.findAllById(scheduleIds).forEach(s -> schedules.put(s.getId(), s));

        Set<String> courseIds = schedules.values().stream().map(CourseSchedule::getCourseId).collect(Collectors.toSet());
        Map<String, Course> courses = new HashMap<>();
        courseRepo.findAllById(courseIds).forEach(c -> courses.put(c.getId(), c));

        return sessions.stream()
                .map(session -> convertToActivePresenceDTO(session, students, schedules, courses))
                .collect(Collectors.toList());
    }

    private ActivePresenceDTO convertToActivePresenceDTO(AttendanceSession session,
            Map<String, Student> students, Map<String, CourseSchedule> schedules, Map<String, Course> courses) {
        ActivePresenceDTO dto = new ActivePresenceDTO();
        dto.setSessionId(session.getId());
        dto.setStudentId(session.getStudentId());
        dto.setEntryTime(session.getEntryTime().toEpochMilli());
        dto.setDurationSeconds((long) session.getTotalDurationSeconds());

        Student student = students.get(session.getStudentId());
        if (student != null) {
            dto.setStudentName(student.getName() + " " + student.getSurname());
        }

        CourseSchedule schedule = schedules.get(session.getCourseScheduleId());
        if (schedule != null) {
            Course course = courses.get(schedule.getCourseId());
            if (course != null) {
                dto.setCourseId(course.getId());
                dto.setCourseName(course.getName());
            }
        }

        return dto;
    }

    public List<AttendanceStatsDTO> getCourseStats(String courseId) {
        return convertStatsToDTO(statsRepo.findByCourseId(courseId));
    }

    public List<AttendanceStatsDTO> getAllCourseStats() {
        return convertStatsToDTO(statsRepo.findAll());
    }

    public List<AttendanceStatsDTO> getStudentStats(String studentId) {
        return convertStatsToDTO(statsRepo.findByStudentId(studentId));
    }

    public DailyAttendanceSummary getStudentDailySummary(String studentId, String courseId, LocalDate date) {
        return summaryRepo.findByStudentIdAndCourseIdAndDate(studentId, courseId, date)
                .orElse(null);
    }

    private List<AttendanceStatsDTO> convertStatsToDTO(List<SemesterAttendanceStats> statsList) {
        if (statsList.isEmpty()) return Collections.emptyList();

        Set<String> studentIds = statsList.stream().map(SemesterAttendanceStats::getStudentId).collect(Collectors.toSet());
        Set<String> courseIds = statsList.stream().map(SemesterAttendanceStats::getCourseId).collect(Collectors.toSet());

        Map<String, Student> students = new HashMap<>();
        studentRepo.findAllById(studentIds).forEach(s -> students.put(s.getId(), s));

        Map<String, Course> courses = new HashMap<>();
        courseRepo.findAllById(courseIds).forEach(c -> courses.put(c.getId(), c));

        return statsList.stream().map(stats -> {
            AttendanceStatsDTO dto = new AttendanceStatsDTO();
            dto.setStudentId(stats.getStudentId());
            dto.setTotalClasses(stats.getTotalClasses());
            dto.setAttendedClasses(stats.getAttendedClasses());
            dto.setLateArrivals(stats.getLateArrivals());
            dto.setAttendancePercentage(stats.getAttendancePercentage());

            Student student = students.get(stats.getStudentId());
            if (student != null) dto.setStudentName(student.getName() + " " + student.getSurname());

            Course course = courses.get(stats.getCourseId());
            if (course != null) {
                dto.setCourseId(course.getId());
                dto.setCourseCode(course.getCode());
                dto.setCourseName(course.getName());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public List<DailyAttendanceSummary> getCourseAttendanceForDate(String courseId, LocalDate date) {
        return summaryRepo.findByCourseIdAndDate(courseId, date);
    }

    public List<AttendanceSession> getStudentHistory(String studentId, String courseId, LocalDate startDate, LocalDate endDate) {
        Instant start = startDate != null ? startDate.atStartOfDay(ZoneId.systemDefault()).toInstant() : Instant.EPOCH;
        Instant end = endDate != null ? endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant() : Instant.now();

        List<AttendanceSession> sessions = sessionRepo.findByStudentIdAndEntryTimeBetween(studentId, start, end);

        if (courseId != null) {
            return sessions.stream()
                    .filter(s -> {
                        return scheduleRepo.findById(s.getCourseScheduleId())
                                .map(schedule -> schedule.getCourseId().equals(courseId))
                                .orElse(false);
                    })
                    .collect(Collectors.toList());
        } else {
            return sessions;
        }
    }

    // Academic year starts in September: FIRST = Sep–Dec, SECOND = Feb–Jun of the following year
    // e.g. Sep 2024–Jun 2025 → academicYear = 2024
    private static int academicYear() {
        LocalDate today = LocalDate.now();
        return today.getMonthValue() >= 9 ? today.getYear() : today.getYear() - 1;
    }

    private static String currentSemester() {
        int month = LocalDate.now().getMonthValue();
        if (month >= 9 && month <= 12) return "FIRST";
        if (month >= 2 && month <= 6)  return "SECOND";
        return "SECOND";
    }

    private static LocalDate semesterStart(String semester) {
        int acYear = academicYear();
        return "SECOND".equals(semester) ? LocalDate.of(acYear + 1, 2, 1) : LocalDate.of(acYear, 9, 1);
    }

    private static LocalDate semesterEnd(String semester) {
        int acYear = academicYear();
        return "SECOND".equals(semester) ? LocalDate.of(acYear + 1, 6, 30) : LocalDate.of(acYear, 12, 30);
    }

    // Count how many times a given day-of-week falls within [start, end] inclusive
    private static long countDayOccurrences(DayOfWeek day, LocalDate start, LocalDate end) {
        LocalDate first = start.with(TemporalAdjusters.nextOrSame(day));
        if (first.isAfter(end)) return 0;
        return ChronoUnit.WEEKS.between(first, end) + 1;
    }
}

