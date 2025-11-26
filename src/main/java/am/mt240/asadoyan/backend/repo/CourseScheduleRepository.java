package am.mt240.asadoyan.backend.repo;

import am.mt240.asadoyan.backend.model.CourseSchedule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

import am.mt240.asadoyan.backend.model.CourseSchedule.ClassPeriod;

@Repository
public interface CourseScheduleRepository extends MongoRepository<CourseSchedule, String> {
    List<CourseSchedule> findByCourseId(String courseId);
    List<CourseSchedule> findByGroupName(String groupName);

    Optional<CourseSchedule> findFirstByRoomIdAndDayOfWeekAndClassPeriod(String roomId, DayOfWeek dayOfWeek, ClassPeriod period);
}

