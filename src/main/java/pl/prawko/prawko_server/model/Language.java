package pl.prawko.prawko_server.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.List;
import java.util.Objects;

/**
 * Represents a {@code Language} entity within the application.
 * <p>
 * A {@code Language} has:
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
 *     <li>{@link QuestionTranslation}: Each language is assigned to multiple question translations.</li>
 *     <li>{@link AnswerTranslation}: Each language is assigned to multiple answer translations.</li>
 *     <li>{@link Exam}: Each language is assigned to multiple exams.</li>
 * </ul>
 * <p>
 * The entity is mapped do the database table {@code question}.
 * All setters are returning {@code  Language} itself, enabling method chaining.
 */
@Entity
public class Language {

    /**
     * Predefined code language for Polish
     */
    public static final String PL = "pl";
    /**
     * Predefined code language for English
     */
    public static final String EN = "en";
    /**
     * Predefined code language for German
     */
    public static final String DE = "de";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(length = 31)
    private String name;

    @Column(length = 2)
    private String code;

    @Column(length = 3)
    private String icon;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "language")
    private List<QuestionTranslation> questionTranslations;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "language")
    private List<AnswerTranslation> answerTranslations;

    @OneToMany(mappedBy = "language")
    private List<Exam> exams;

    public long getId() {
        return id;
    }

    public Language setId(final long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Language setName(final String name) {
        this.name = name;
        return this;
    }

    public String getCode() {
        return code;
    }

    public Language setCode(final String code) {
        this.code = code;
        return this;
    }

    public String getIcon() {
        return icon;
    }

    public Language setIcon(final String icon) {
        this.icon = icon;
        return this;
    }

    public List<QuestionTranslation> getQuestionTranslations() {
        return questionTranslations;
    }

    public Language setQuestionTranslations(final List<QuestionTranslation> questionTranslations) {
        this.questionTranslations = questionTranslations;
        return this;
    }

    public List<AnswerTranslation> getAnswerTranslations() {
        return answerTranslations;
    }

    public Language setAnswerTranslations(final List<AnswerTranslation> answerTranslations) {
        this.answerTranslations = answerTranslations;
        return this;
    }

    public List<Exam> getExams() {
        return exams;
    }

    public Language setExams(List<Exam> exams) {
        this.exams = exams;
        return this;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final var language = (Language) object;
        return id == language.id
                && Objects.equals(name, language.name)
                && Objects.equals(code, language.code)
                && Objects.equals(icon, language.icon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, code, icon);
    }

    @Override
    public String toString() {
        return "Language{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", icon='" + icon + '\'' +
                '}';
    }

}
