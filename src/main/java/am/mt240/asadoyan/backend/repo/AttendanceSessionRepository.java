package am.mt240.asadoyan.backend.repo;

import am.mt240.asadoyan.backend.model.AttendanceSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AttendanceSessionRepository extends MongoRepository<AttendanceSession, String> {
    List<AttendanceSession> findByStatus(AttendanceSession.SessionStatus status);
    List<AttendanceSession> findByRoomIdAndStatus(String roomId, AttendanceSession.SessionStatus status);
    List<AttendanceSession> findByStudentIdAndEntryTimeBetween(String studentId, Instant start, Instant end);
    List<AttendanceSession> findByCourseScheduleId(String courseScheduleId);
    List<AttendanceSession> findByCourseScheduleIdAndEntryTimeBetween(String courseScheduleId, Instant start, Instant end);
}

