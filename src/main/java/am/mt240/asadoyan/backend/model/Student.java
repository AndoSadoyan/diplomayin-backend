package am.mt240.asadoyan.backend.model;


import jakarta.validation.constraints.*;
import org.springframework.data.annotation.Id;

import java.time.Instant;


public class Student {
    @Id
    private String id;
    @NotEmpty
    private String name;
    @NotEmpty
    private String surname;
    private String patronymic;
    @NotEmpty
    private String group;
    @Positive
    @Max(4)
    private int subgroup;
    @Positive
    @Max(4)
    private int uniYear;
    @NotNull
    private Instant birthday;
    @Size(min = 512, max = 512)
    private Float[] faceEmbedding;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPatronymic() {
        return patronymic;
    }

    public void setPatronymic(String patronymic) {
        this.patronymic = patronymic;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Integer getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(int subgroup) {
        this.subgroup = subgroup;
    }

    public int getUniYear() {
        return uniYear;
    }

    public void setUniYear(int uniYear) {
        this.uniYear = uniYear;
    }

    public Instant getBirthday() {
        return birthday;
    }

    public void setBirthday(Instant birthday) {
        this.birthday = birthday;
    }

    public Float[] getFaceEmbedding() {
        return faceEmbedding;
    }

    public void setFaceEmbedding(Float[] faceEmbedding) {
        this.faceEmbedding = faceEmbedding;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
