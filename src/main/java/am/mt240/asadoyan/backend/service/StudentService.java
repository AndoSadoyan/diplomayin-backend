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
import am.mt240.asadoyan.backend.model.CourseSchedule.ClassPeriod;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private CourseScheduleRepository scheduleRepository;
    @Autowired
    private Validator validator;

    public List<Student> getAll() {
        return studentRepository.findAll();
    }

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

    public void delete(String id) {
        if (!studentRepository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Student with id " + id);
        studentRepository.deleteById(id);
    }

    public Student edit(String id, Student patched) {
        if (!studentRepository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Student with id " + id);

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

    private List<Student> getStudentsWithCurrentClassInRoom(String roomId) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        DayOfWeek currentDay = now.getDayOfWeek();
        ClassPeriod currentPeriod = ClassPeriod.getCurrentPeriod();

        Optional<CourseSchedule> activeSchedule = scheduleRepository.findFirstByRoomIdAndDayOfWeekAndClassPeriod(
                roomId, currentDay, currentPeriod);

        if (activeSchedule.isEmpty()) {
            return Collections.emptyList();
        }

        String targetGroup = activeSchedule.get().getGroupName();
        Integer targetSubGroup = activeSchedule.get().getSubgroup();
        List<Student> eligibleStudents = studentRepository.findAllFaceEmbeddingsByGroup(targetGroup);
        if(targetSubGroup != null) {
            return eligibleStudents.stream().
                    filter(student -> student.getSubgroup().equals(targetSubGroup))
                    .toList();
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
