package am.mt240.asadoyan.backend.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "daily_attendance_summary")
public class DailyAttendanceSummary {
    @Id
    private String id;
    
    @NotEmpty
    private String studentId;
    
    @NotEmpty
    private String courseId;
    
    @NotNull
    private LocalDate date;
    
    private AttendanceStatus status = AttendanceStatus.ABSENT;
    
    private Boolean wasLate = false;
    
    private Integer totalDurationSeconds = 0;
    
    private Integer expectedDurationSeconds = 0;
    
    private String notes;
    
    public enum AttendanceStatus {
        PRESENT, ABSENT, LATE, EXCUSED
    }

    public DailyAttendanceSummary() {
    }

    public DailyAttendanceSummary(String studentId, String courseId, LocalDate date) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public Boolean getWasLate() {
        return wasLate;
    }

    public void setWasLate(Boolean wasLate) {
        this.wasLate = wasLate;
    }

    public Integer getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public void setTotalDurationSeconds(Integer totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public Integer getExpectedDurationSeconds() {
        return expectedDurationSeconds;
    }

    public void setExpectedDurationSeconds(Integer expectedDurationSeconds) {
        this.expectedDurationSeconds = expectedDurationSeconds;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public double getAttendancePercentage() {
        if (expectedDurationSeconds == 0) return 0.0;
        return (totalDurationSeconds * 100.0) / expectedDurationSeconds;
    }
}

