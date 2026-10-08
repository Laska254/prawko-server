package pl.prawko.prawko_server.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

import java.util.List;

/**
 * Represents a {@code Category} entity within the application.
 * <p>
 * A {@code Language} has:
 *  <ul>
 *     <li>{@code id} - generated unique identifier</li>
 *     <li>{@code name} - limited to 3 characters</li>
 * </ul>
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link Question}: Each {@code category} can have multiple questions</li>
 *     <li>{@link Exam}: {@code Category} can be assigned to multiple exams</li>
 * </ul>
 * <p>
 * The entity is mapped do the database table {@code category}.
 * All setters are returning {@code  Language} itself, enabling method chaining.
 */
@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(length = 3)
    private String name;

    @ManyToMany(mappedBy = "categories")
    @JsonBackReference
    private List<Question> questions;

    @OneToMany(mappedBy = "category")
    private List<Exam> exams;

    public long getId() {
        return id;
    }

    public Category setId(final long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Category setName(final String name) {
        this.name = name;
        return this;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public Category setQuestions(final List<Question> questions) {
        this.questions = questions;
        return this;
    }

    public List<Exam> getExams() {
        return exams;
    }

    public Category setExams(final List<Exam> exams) {
        this.exams = exams;
        return this;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Category other)) {
            return false;
        }
        return id != 0 && id == other.getId();
    }

    @Override
    public int hashCode() {
        return Category.class.hashCode();
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

}
