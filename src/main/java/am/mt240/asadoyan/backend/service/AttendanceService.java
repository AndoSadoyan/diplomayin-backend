package am.mt240.asadoyan.backend.service;

import am.mt240.asadoyan.backend.dto.*;
import am.mt240.asadoyan.backend.model.*;
import am.mt240.asadoyan.backend.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import am.mt240.asadoyan.backend.model.CourseSchedule.ClassPeriod;

import java.time.*;
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

        summary.setTotalDurationSeconds(
                summary.getTotalDurationSeconds() + session.getTotalDurationSeconds()
        );

        LocalTime arrivalTime = entryZdt.toLocalTime();
        LocalTime graceEnd = schedule.getStartTime().plusMinutes(15);
        summary.setWasLate(arrivalTime.isAfter(graceEnd));

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
        String semester = course.getSemester();

        long totalClasses = scheduleRepo.findByCourseId(courseId).size() * 15;

        List<DailyAttendanceSummary> summaries = summaryRepo.findByStudentIdAndCourseId(studentId, courseId);
        long attendedClasses = summaries.stream()
                .filter(s -> s.getStatus() == DailyAttendanceSummary.AttendanceStatus.PRESENT ||
                        s.getStatus() == DailyAttendanceSummary.AttendanceStatus.LATE)
                .count();
        long lateArrivals = summaries.stream()
                .filter(s -> s.getStatus() == DailyAttendanceSummary.AttendanceStatus.LATE)
                .count();

        double percentage = totalClasses > 0 ? (attendedClasses * 100.0 / totalClasses) : 0.0;

        SemesterAttendanceStats stats = statsRepo
                .findByStudentIdAndCourseIdAndSemester(studentId, courseId, semester)
                .orElse(new SemesterAttendanceStats(studentId, courseId, semester));

        stats.setTotalClasses((int) totalClasses);
        stats.setAttendedClasses((int) attendedClasses);
        stats.setLateArrivals((int) lateArrivals);
        stats.setAttendancePercentage(percentage);

        statsRepo.save(stats);
    }

    public List<ActivePresenceDTO> getActivePresence(String roomId) {
        List<AttendanceSession> activeSessions = sessionRepo.findByRoomIdAndStatus(
                roomId,
                AttendanceSession.SessionStatus.ACTIVE
        );

        return activeSessions.stream()
                .map(this::convertToActivePresenceDTO)
                .collect(Collectors.toList());
    }

    public Map<String, List<ActivePresenceDTO>> getAllActivePresence() {
        List<AttendanceSession> activeSessions = sessionRepo.findByStatus(
                AttendanceSession.SessionStatus.ACTIVE
        );

        return activeSessions.stream()
                .map(this::convertToActivePresenceDTO)
                .collect(Collectors.groupingBy(dto -> {
                    return activeSessions.stream()
                            .filter(s -> s.getId().equals(dto.getSessionId()))
                            .findFirst()
                            .map(AttendanceSession::getRoomId)
                            .orElse("Unknown");
                }));
    }

    private ActivePresenceDTO convertToActivePresenceDTO(AttendanceSession session) {
        ActivePresenceDTO dto = new ActivePresenceDTO();
        dto.setSessionId(session.getId());
        dto.setStudentId(session.getStudentId());
        dto.setEntryTime(session.getEntryTime().toEpochMilli());
        dto.setDurationSeconds((long) session.getTotalDurationSeconds());

        studentRepo.findById(session.getStudentId()).ifPresent(student -> {
            dto.setStudentName(student.getName() + " " + student.getSurname());
        });

        scheduleRepo.findById(session.getCourseScheduleId()).ifPresent(schedule -> {
            courseRepo.findById(schedule.getCourseId()).ifPresent(course -> {
                dto.setCourseId(course.getId());
                dto.setCourseName(course.getName());
            });
        });

        return dto;
    }

    public List<AttendanceStatsDTO> getCourseStats(String courseId) {
        List<SemesterAttendanceStats> statsList = statsRepo.findByCourseId(courseId);

        return statsList.stream()
                .map(this::convertToAttendanceStatsDTO)
                .collect(Collectors.toList());
    }

    public List<AttendanceStatsDTO> getStudentStats(String studentId) {
        List<SemesterAttendanceStats> statsList = statsRepo.findByStudentId(studentId);

        return statsList.stream()
                .map(this::convertToAttendanceStatsDTO)
                .collect(Collectors.toList());
    }

    public DailyAttendanceSummary getStudentDailySummary(String studentId, String courseId, LocalDate date) {
        return summaryRepo.findByStudentIdAndCourseIdAndDate(studentId, courseId, date)
                .orElse(null);
    }

    private AttendanceStatsDTO convertToAttendanceStatsDTO(SemesterAttendanceStats stats) {
        AttendanceStatsDTO dto = new AttendanceStatsDTO();
        dto.setStudentId(stats.getStudentId());
        dto.setTotalClasses(stats.getTotalClasses());
        dto.setAttendedClasses(stats.getAttendedClasses());
        dto.setLateArrivals(stats.getLateArrivals());
        dto.setAttendancePercentage(stats.getAttendancePercentage());

        studentRepo.findById(stats.getStudentId()).ifPresent(student -> {
            dto.setStudentName(student.getName() + " " + student.getSurname());
        });

        courseRepo.findById(stats.getCourseId()).ifPresent(course -> {
            dto.setCourseCode(course.getCode());
            dto.setCourseName(course.getName());
        });

        return dto;
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
}

