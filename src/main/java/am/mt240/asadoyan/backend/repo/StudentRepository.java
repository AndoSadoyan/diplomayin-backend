package am.mt240.asadoyan.backend.repo;

import am.mt240.asadoyan.backend.model.Student;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface StudentRepository extends MongoRepository<Student, String> {

    @Query(value = "{'faceEmbedding': { $exists: true }}", fields = "{ '_id' : 1, 'faceEmbedding' : 1 }")
    List<Student> findAllFaceEmbeddings();

    @Query(value = "{'faceEmbedding': { $exists: true }, 'group': ?0}", fields = "{ '_id' : 1, 'faceEmbedding' : 1, 'group' : 1, 'subgroup' : 1 }")
    List<Student> findAllFaceEmbeddingsByGroup(String group);

    List<Student> findByGroupAndSurname(String group, String surname);
}
