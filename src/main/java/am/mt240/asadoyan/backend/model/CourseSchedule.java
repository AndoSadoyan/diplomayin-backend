package am.mt240.asadoyan.backend.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Document(collection = "course_schedules")
public class CourseSchedule {
    @Id
    private String id;
    @NotEmpty
    private String courseId;
    @NotEmpty
    private String roomId;
    @NotEmpty
    private String groupName;
    private Integer subgroup;
    @NotEmpty
    private String instructorName;
    @NotNull
    private DayOfWeek dayOfWeek;
    @NotNull
    private ClassPeriod classPeriod;

    public enum ClassPeriod {
        FIRST(LocalTime.of(23, 30), LocalTime.of(23, 50)),
        SECOND(LocalTime.of(11, 0), LocalTime.of(12, 20)),
        THIRD(LocalTime.of(12, 50), LocalTime.of(14, 10)),
        FORTH(LocalTime.of(14, 20), LocalTime.of(15, 40)),
        FIFTH(LocalTime.of(15, 50), LocalTime.of(17, 10));

        private final LocalTime start;
        private final LocalTime end;

        ClassPeriod(LocalTime start, LocalTime end){
            this.start = start;
            this.end = end;
        }

        public LocalTime getStart() {
            return start;
        }

        public LocalTime getEnd() {
            return end;
        }

        public static ClassPeriod getCurrentPeriod() {
            LocalTime now = LocalTime.now();
            for(ClassPeriod period : values()) {
                if(now.isAfter(period.start) && now.isBefore(period.end))
                    return period;
            }
            return null;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Integer getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(Integer subgroup) {
        this.subgroup = subgroup;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public ClassPeriod getClassPeriod() {
        return classPeriod;
    }

    public void setClassPeriod(ClassPeriod lessonNumber) {
        this.classPeriod = lessonNumber;
    }

    public LocalTime getStartTime(){
        return classPeriod.getStart();
    }
    public LocalTime getEndTime() {
        return classPeriod.getEnd();
    }
}

