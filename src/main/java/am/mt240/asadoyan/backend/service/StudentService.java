package am.mt240.asadoyan.backend.service;

import am.mt240.asadoyan.backend.model.CourseSchedule;
import am.mt240.asadoyan.backend.model.Student;
import am.mt240.asadoyan.backend.repo.CourseScheduleRepository;
import am.mt240.asadoyan.backend.repo.StudentRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private CourseScheduleRepository scheduleRepository;
    @Autowired
    private Validator validator;

    public Student get(String id) {
        if (!studentRepository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Student with id " + id);

        return studentRepository.findById(id).get();
    }

    public Student add(Student student) {
        if (student.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Do not specify id when creating");
        }

        return studentRepository.save(student);
    }

    public Student update(Student student) {
        if (student.getId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must specify an id to update");

        if (!studentRepository.existsById(student.getId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Student with id " + student.getId());

        return studentRepository.save(student);
    }

    public Student edit(String id, Student patched) {
        if (!studentRepository.existsById(patched.getId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Student with id " + patched.getId());

        Student saved = studentRepository.findById(id).get();

        saved.setName(patched.getName() == null ? saved.getName() : patched.getName());
        saved.setSurname(patched.getSurname() == null ? saved.getSurname() : patched.getSurname());
        saved.setPatronymic(patched.getPatronymic() == null ? saved.getPatronymic() : patched.getPatronymic());
        saved.setGroup(patched.getGroup() == null ? saved.getGroup() : patched.getGroup());
        saved.setUniYear(patched.getUniYear() == 0 ? saved.getUniYear() : patched.getUniYear());
        saved.setSubgroup(patched.getSubgroup() == null ? saved.getSubgroup() : patched.getSubgroup());
        saved.setBirthday(patched.getBirthday() == null ? saved.getBirthday() : patched.getBirthday());
        saved.setFaceEmbedding(patched.getFaceEmbedding() == null ? saved.getFaceEmbedding() : patched.getFaceEmbedding());

        validate(saved);
        return studentRepository.save(saved);
    }

    public void addEmbedding(String id, Float[] embedding) {
        if (!studentRepository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Student with id " + id);

        Student student = studentRepository.findById(id).get();
        student.setFaceEmbedding(embedding);
        studentRepository.save(student);
    }

    public Map<String, Float[]> getEmbeddings(String group, String roomId) {
        List<Student> students;
        
        // If roomId is provided, filter by students who have class in that room RIGHT NOW
        if (roomId != null) {
            students = getStudentsWithCurrentClassInRoom(roomId);
        } else if (group != null) {
            students = studentRepository.findAllFaceEmbeddingsByGroup(group);
        } else {
            students = studentRepository.findAllFaceEmbeddings();
        }

        Map<String, Float[]> embeddingMap = new HashMap<>();
        for(Student student : students) {
            if (student.getFaceEmbedding() != null) {
                embeddingMap.put(student.getId(), student.getFaceEmbedding());
            }
        }

        return embeddingMap;
    }

    /**
     * Get students who have a scheduled class in the given room at the current time
     */
    private List<Student> getStudentsWithCurrentClassInRoom(String roomId) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        DayOfWeek currentDay = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();

        // Find schedules for this room at this time
        List<CourseSchedule> activeSchedules = scheduleRepository.findAll().stream()
                .filter(schedule -> 
                    schedule.getRoomId().equals(roomId) &&
                    schedule.getDayOfWeek() == currentDay &&
                    !currentTime.isBefore(schedule.getClassPeriod().getStart()) &&
                    !currentTime.isAfter(schedule.getClassPeriod().getEnd())
                )
                .collect(Collectors.toList());

        if (activeSchedules.isEmpty()) {
            return Collections.emptyList();
        }

        // Get all students whose group matches any of the active schedules
        Set<String> targetGroups = activeSchedules.stream()
                .map(CourseSchedule::getGroupName)
                .collect(Collectors.toSet());

        List<Student> eligibleStudents = new ArrayList<>();
        for (String groupName : targetGroups) {
            List<Student> groupStudents = studentRepository.findAllFaceEmbeddingsByGroup(groupName);

            for (Student student : groupStudents) {
                boolean matchesAnySchedule = activeSchedules.stream()
                        .anyMatch(schedule -> 
                            schedule.getGroupName().equals(student.getGroup()) &&
                            (schedule.getSubgroup() == null || schedule.getSubgroup().equals(student.getSubgroup()))
                        );
                
                if (matchesAnySchedule && student.getFaceEmbedding() != null) {
                    eligibleStudents.add(student);
                }
            }
        }

        return eligibleStudents;
    }

    private void validate(Student student) {
        Set<ConstraintViolation<Student>> violations = validator.validate(student);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

}
