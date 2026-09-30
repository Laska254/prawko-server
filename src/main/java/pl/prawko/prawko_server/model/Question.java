package pl.prawko.prawko_server.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

import java.util.List;

/**
 * Represents a {@code Question} entity within the application.
 * <p>
 * A {@code Question} has:
 *  <ul>
 *     <li>{@code id} - fixed unique identifier</li>
 *     <li>{@code name} - limited to 7 characters</li>
 *     <li>{@code points} - number of given points for correct answer to a question</li>
 *     <li>{@code media} - filename of media attached to a question</li>
 *     <li>{@code type} - type of question represented by {@link QuestionType}</li>
 * </ul>
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link QuestionTranslation}: A question can have multiple translations.</li>
 *     <li>{@link Answer}: A question have two or three answers.</li>
 * </ul>
 * The entity is mapped to the database table {@code question}.
 * All setters are returning {@code Question} itself, enabling method chaining.
 */
@Entity
public class Question {

    @Id
    private long id;

    @Column(length = 7)
    private String name;

    private int points;

    @Column(length = 63)
    private String media;

    @Enumerated(EnumType.STRING)
    private QuestionType type;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "question")
    private List<QuestionTranslation> translations;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "question")
    private List<Answer> answers;

    @ManyToMany
    @JoinTable(
            name = "question_category",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> categories;

    @ManyToMany(mappedBy = "questions", cascade = CascadeType.ALL)
    private List<Exam> exams;

    public long getId() {
        return id;
    }

    public Question setId(final long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Question setName(final String name) {
        this.name = name;
        return this;
    }

    public int getPoints() {
        return points;
    }

    public Question setPoints(final int points) {
        this.points = points;
        return this;
    }

    public List<QuestionTranslation> getTranslations() {
        return translations;
    }

    public Question setTranslations(final List<QuestionTranslation> translations) {
        this.translations = translations;
        return this;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public Question setAnswers(final List<Answer> answers) {
        this.answers = answers;
        return this;
    }

    public String getMedia() {
        return media;
    }

    public Question setMedia(final String media) {
        this.media = media;
        return this;
    }

    public QuestionType getType() {
        return type;
    }

    public Question setType(final QuestionType type) {
        this.type = type;
        return this;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public Question setCategories(final List<Category> categories) {
        this.categories = categories;
        return this;
    }

    public List<Exam> getExams() {
        return exams;
    }

    public Question setExams(final List<Exam> exams) {
        this.exams = exams;
        return this;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Question other)) {
            return false;
        }
        return id != 0 && id == other.getId();
    }

    @Override
    public int hashCode() {
        return Question.class.hashCode();
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", points=" + points +
                ", media='" + media + '\'' +
                ", type=" + type +
                '}';
    }

}
