package am.mt240.asadoyan.backend.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "courses")
public class Course {
    @Id
    private String id;
    
    @NotEmpty
    private String name;
    
    @NotEmpty
    private String code; // e.g., "DB-1", "DSA-1" (subject-yearLevel)
    
    @NotEmpty
    private String semester; // e.g., "Fall 2024", "Spring 2025"
    
    @NotNull
    @Positive
    private Integer year;
    
    private Integer yearLevel; // 1, 2, 3, 4 - which year students take this course
    
    private String description;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getYearLevel() {
        return yearLevel;
    }

    public void setYearLevel(Integer yearLevel) {
        this.yearLevel = yearLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

