package am.mt240.asadoyan.backend.model;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "semester_attendance_stats")
public class SemesterAttendanceStats {
    @Id
    private String id;
    
    @NotEmpty
    private String studentId;
    
    @NotEmpty
    private String courseId;
    
    @NotEmpty
    private String semester;
    
    private Integer totalClasses = 0;
    
    private Integer attendedClasses = 0;
    
    private Integer lateArrivals = 0;
    
    private Double attendancePercentage = 0.0;
    
    private Instant lastUpdated;

    public SemesterAttendanceStats() {
        this.lastUpdated = Instant.now();
    }

    public SemesterAttendanceStats(String studentId, String courseId, String semester) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.semester = semester;
        this.lastUpdated = Instant.now();
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

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Integer getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(Integer totalClasses) {
        this.totalClasses = totalClasses;
        this.lastUpdated = Instant.now();
    }

    public Integer getAttendedClasses() {
        return attendedClasses;
    }

    public void setAttendedClasses(Integer attendedClasses) {
        this.attendedClasses = attendedClasses;
        this.lastUpdated = Instant.now();
    }

    public Integer getLateArrivals() {
        return lateArrivals;
    }

    public void setLateArrivals(Integer lateArrivals) {
        this.lateArrivals = lateArrivals;
        this.lastUpdated = Instant.now();
    }

    public Double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(Double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
        this.lastUpdated = Instant.now();
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}

