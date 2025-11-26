package am.mt240.asadoyan.backend.controller;

import am.mt240.asadoyan.backend.model.Student;
import am.mt240.asadoyan.backend.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @PostMapping()
    public Student addStudent(@Valid @RequestBody Student student) {
        return studentService.add(student);
    }

    @GetMapping("/{id}")
    public Student getStudent(@PathVariable String id) {
        return studentService.get(id);
    }

    @PutMapping()
    public Student updateStudent(@Valid @RequestBody Student student) {
        return studentService.update(student);
    }

    @PatchMapping("/{id}")
    public Student editStudent(@PathVariable String id, @RequestBody Student patches) {
        return studentService.edit(id, patches);
    }

    @PostMapping("/{id}/registerFace")
    public void registerFace(@PathVariable String id, @RequestBody Float[] embedding) {
        studentService.addEmbedding(id, embedding);
    }

    @GetMapping("/embeddings")
    public Map<String, Float[]> getEmbeddingsMap(
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String roomId) {
        return studentService.getEmbeddings(group, roomId);
    }

}
