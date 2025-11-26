package am.mt240.asadoyan.backend.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "attendance_sessions")
public class AttendanceSession {
    @Id
    private String id;
    
    @NotEmpty
    private String studentId;
    @NotEmpty
    private String roomId;
    
    @NotNull
    private String courseScheduleId;
    
    @NotNull
    private Instant entryTime;
    @NotNull
    private Instant lastSeen;
    
    private Instant exitTime;
    
    private Integer totalDurationSeconds = 0;
    
    private SessionStatus status = SessionStatus.ACTIVE;
    
    private Float avgConfidenceScore;
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    public enum SessionStatus {
        ACTIVE, COMPLETED, INTERRUPTED
    }

    public AttendanceSession() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
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

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getCourseScheduleId() {
        return courseScheduleId;
    }

    public void setCourseScheduleId(String courseScheduleId) {
        this.courseScheduleId = courseScheduleId;
    }

    public Instant getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(Instant entryTime) {
        this.entryTime = entryTime;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
        this.updatedAt = Instant.now();
    }

    public Instant getExitTime() {
        return exitTime;
    }

    public void setExitTime(Instant exitTime) {
        this.exitTime = exitTime;
        this.updatedAt = Instant.now();
    }

    public Integer getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public void setTotalDurationSeconds(Integer totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Float getAvgConfidenceScore() {
        return avgConfidenceScore;
    }

    public void setAvgConfidenceScore(Float avgConfidenceScore) {
        this.avgConfidenceScore = avgConfidenceScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

