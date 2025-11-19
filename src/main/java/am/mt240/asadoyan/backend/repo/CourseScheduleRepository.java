package am.mt240.asadoyan.backend.repo;

import am.mt240.asadoyan.backend.model.CourseSchedule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseScheduleRepository extends MongoRepository<CourseSchedule, String> {
    List<CourseSchedule> findByCourseId(String courseId);
    List<CourseSchedule> findByGroupName(String groupName);
    
    @Query("{ 'roomId': ?0, 'dayOfWeek': ?1, 'startTime': { $lte: ?2 }, 'endTime': { $gte: ?2 } }")
    Optional<CourseSchedule> findByRoomAndDayAndTime(String roomId, DayOfWeek dayOfWeek, LocalTime time);
}

