package am.mt240.asadoyan.backend.repo;

import am.mt240.asadoyan.backend.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface CourseRepository extends MongoRepository<Course, String> {
    Optional<Course> findByCode(String code);
}

