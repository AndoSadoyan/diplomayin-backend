package am.mt240.asadoyan.backend;

import am.mt240.asadoyan.backend.model.Student;
import am.mt240.asadoyan.backend.repo.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DiplomayinBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiplomayinBackendApplication.class, args);
    }

}
