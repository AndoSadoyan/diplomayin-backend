package am.mt240.asadoyan.backend.dto;

public class AttendanceStatsDTO {
    private String studentId;
    private String studentName;
    private String courseCode;
    private String courseName;
    private Integer totalClasses;
    private Integer attendedClasses;
    private Integer lateArrivals;
    private Double attendancePercentage;

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Integer getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(Integer totalClasses) {
        this.totalClasses = totalClasses;
    }

    public Integer getAttendedClasses() {
        return attendedClasses;
    }

    public void setAttendedClasses(Integer attendedClasses) {
        this.attendedClasses = attendedClasses;
    }

    public Integer getLateArrivals() {
        return lateArrivals;
    }

    public void setLateArrivals(Integer lateArrivals) {
        this.lateArrivals = lateArrivals;
    }

    public Double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(Double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }
}

