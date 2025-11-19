package am.mt240.asadoyan.backend.repo;

import am.mt240.asadoyan.backend.model.DailyAttendanceSummary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyAttendanceSummaryRepository extends MongoRepository<DailyAttendanceSummary, String> {
    Optional<DailyAttendanceSummary> findByStudentIdAndCourseIdAndDate(String studentId, String courseId, LocalDate date);
    List<DailyAttendanceSummary> findByCourseIdAndDate(String courseId, LocalDate date);
    List<DailyAttendanceSummary> findByStudentIdAndCourseId(String studentId, String courseId);
}

