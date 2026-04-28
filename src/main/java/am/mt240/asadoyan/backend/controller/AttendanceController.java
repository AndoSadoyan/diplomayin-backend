package am.mt240.asadoyan.backend.controller;

import am.mt240.asadoyan.backend.dto.*;
import am.mt240.asadoyan.backend.model.AttendanceSession;
import am.mt240.asadoyan.backend.model.DailyAttendanceSummary;
import am.mt240.asadoyan.backend.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/checkin")
    public ResponseEntity<CheckinResponse> checkin(@Valid @RequestBody CheckinRequest request) {
        CheckinResponse response = attendanceService.checkin(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@Valid @RequestBody HeartbeatRequest request) {
        attendanceService.heartbeat(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<Long> checkout(@Valid @RequestBody CheckoutRequest request) {
        Long duration = attendanceService.checkout(request);
        return ResponseEntity.ok(duration);
    }

    @GetMapping("/active/{roomId}")
    public ResponseEntity<List<ActivePresenceDTO>> getActivePresence(@PathVariable String roomId) {
        List<ActivePresenceDTO> activePresence = attendanceService.getActivePresence(roomId);
        return ResponseEntity.ok(activePresence);
    }

    @GetMapping("/active/all")
    public ResponseEntity<Map<String, List<ActivePresenceDTO>>> getAllActivePresence() {
        Map<String, List<ActivePresenceDTO>> allActive = attendanceService.getAllActivePresence();
        return ResponseEntity.ok(allActive);
    }

    @GetMapping("/student/{studentId}/history")
    public ResponseEntity<List<AttendanceSession>> getStudentHistory(
            @PathVariable String studentId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceSession> history = attendanceService.getStudentHistory(studentId, courseId, startDate, endDate);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/course/{courseId}/daily")
    public ResponseEntity<List<DailyAttendanceSummary>> getCourseAttendance(
            @PathVariable String courseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DailyAttendanceSummary> attendance = attendanceService.getCourseAttendanceForDate(courseId, date);
        return ResponseEntity.ok(attendance);
    }

    @GetMapping("/course/{courseId}/stats")
    public ResponseEntity<List<AttendanceStatsDTO>> getCourseStats(@PathVariable String courseId) {
        return ResponseEntity.ok(attendanceService.getCourseStats(courseId));
    }

    @GetMapping("/stats/courses")
    public ResponseEntity<List<AttendanceStatsDTO>> getAllCourseStats() {
        return ResponseEntity.ok(attendanceService.getAllCourseStats());
    }

    @PostMapping("/course/{courseId}/excuse")
    public ResponseEntity<Void> excuseCourse(
            @PathVariable String courseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        attendanceService.excuseCourse(courseId, date);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/student/{studentId}/excuse")
    public ResponseEntity<Void> excuseStudent(
            @PathVariable String studentId,
            @RequestParam String courseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        attendanceService.excuseStudent(studentId, courseId, startDate, endDate);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/student/{studentId}/stats")
    public ResponseEntity<List<AttendanceStatsDTO>> getStudentStats(@PathVariable String studentId) {
        List<AttendanceStatsDTO> stats = attendanceService.getStudentStats(studentId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/student/{studentId}/daily/{courseId}")
    public ResponseEntity<DailyAttendanceSummary> getStudentDailySummary(
            @PathVariable String studentId,
            @PathVariable String courseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyAttendanceSummary summary = attendanceService.getStudentDailySummary(studentId, courseId, date);
        return ResponseEntity.ok(summary);
    }
}

