package am.mt240.asadoyan.backend.controller;

import am.mt240.asadoyan.backend.model.Course;
import am.mt240.asadoyan.backend.model.CourseSchedule;
import am.mt240.asadoyan.backend.model.Student;
import am.mt240.asadoyan.backend.repo.CourseRepository;
import am.mt240.asadoyan.backend.repo.CourseScheduleRepository;
import am.mt240.asadoyan.backend.repo.StudentRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseRepository courseRepo;

    @Autowired
    private CourseScheduleRepository scheduleRepo;

    @Autowired
    private StudentRepository studentRepo;

    @PostMapping
    public ResponseEntity<Course> createCourse(@Valid @RequestBody Course course) {
        Course saved = courseRepo.save(course);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourse(@PathVariable String id) {
        return courseRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseRepo.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable String id, @Valid @RequestBody Course course) {
        if (!courseRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        course.setId(id);
        Course updated = courseRepo.save(course);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id) {
        if (!courseRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        courseRepo.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Course>> getStudentCourses(@PathVariable String studentId) {
        Student student = studentRepo.findById(studentId).orElse(null);
        if (student == null) {
            return ResponseEntity.notFound().build();
        }

        // Find all schedules for this student's group
        List<CourseSchedule> schedules = scheduleRepo.findByGroupName(student.getGroup());
        
        // Filter by subgroup
        schedules = schedules.stream()
                .filter(s -> s.getSubgroup() == null || s.getSubgroup().equals(student.getSubgroup()))
                .toList();
        
        // Get unique course IDs
        List<String> courseIds = schedules.stream()
                .map(CourseSchedule::getCourseId)
                .distinct()
                .toList();
        
        // Fetch courses
        List<Course> courses = courseIds.stream()
                .map(id -> courseRepo.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        
        return ResponseEntity.ok(courses);
    }

    @PostMapping("/{courseId}/schedule")
    public ResponseEntity<CourseSchedule> addSchedule(
            @PathVariable String courseId,
            @Valid @RequestBody CourseSchedule schedule) {
        schedule.setCourseId(courseId);
        CourseSchedule saved = scheduleRepo.save(schedule);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{courseId}/schedule")
    public ResponseEntity<List<CourseSchedule>> getCourseSchedule(@PathVariable String courseId) {
        return ResponseEntity.ok(scheduleRepo.findByCourseId(courseId));
    }

    @GetMapping("/schedules/all")
    public ResponseEntity<List<CourseSchedule>> getAllSchedules() {
        return ResponseEntity.ok(scheduleRepo.findAll());
    }

    @DeleteMapping("/schedule/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable String scheduleId) {
        if (!scheduleRepo.existsById(scheduleId)) {
            return ResponseEntity.notFound().build();
        }
        scheduleRepo.deleteById(scheduleId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/schedule/room/{roomId}")
    public ResponseEntity<List<CourseSchedule>> getRoomSchedules(@PathVariable String roomId) {
        List<CourseSchedule> roomSchedules = scheduleRepo.findAll().stream()
                .filter(s -> s.getRoomId().equals(roomId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(roomSchedules);
    }
}

