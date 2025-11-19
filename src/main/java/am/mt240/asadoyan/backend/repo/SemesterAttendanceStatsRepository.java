package am.mt240.asadoyan.backend.repo;

import am.mt240.asadoyan.backend.model.SemesterAttendanceStats;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterAttendanceStatsRepository extends MongoRepository<SemesterAttendanceStats, String> {
    Optional<SemesterAttendanceStats> findByStudentIdAndCourseIdAndSemester(String studentId, String courseId, String semester);
    List<SemesterAttendanceStats> findByStudentId(String studentId);
    List<SemesterAttendanceStats> findByCourseId(String courseId);
}

