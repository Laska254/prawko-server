package pl.prawko.prawko_server.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.List;
import java.util.Objects;

/**
 * Represents a {@code Answer} entity within the application.
 * <p>
 * An {@code Answer} has:
 *  <ul>
 *     <li>{@code id} - generated unique identifier</li>
 *     <li>{@code correct} - indicates if this answer is correct</li>
 * </ul>
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link Question}: Multiple answers belong to a single question</li>
 *     <li>{@link AnswerTranslation}: An answer can have multiple translations</li>
 *     <li>{@link Exam}: An answer can be associated with multiple exams</li>
 * </ul>
 * The entity is mapped to the database table {@code answer}.
 * All setters are returning {@code Answer} itself, enabling method chaining.
 */
@Entity
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private boolean correct;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL)
    private List<AnswerTranslation> translations;

    @ManyToMany(mappedBy = "userAnswers", cascade = CascadeType.ALL)
    @JsonBackReference
    private List<Exam> tests;

    public long getId() {
        return id;
    }

    public Answer setId(final long id) {
        this.id = id;
        return this;
    }

    public boolean isCorrect() {
        return correct;
    }

    public Answer setCorrect(final boolean correct) {
        this.correct = correct;
        return this;
    }

    public Question getQuestion() {
        return question;
    }

    public Answer setQuestion(final Question question) {
        this.question = question;
        return this;
    }

    public List<AnswerTranslation> getTranslations() {
        return translations;
    }

    public Answer setTranslations(final List<AnswerTranslation> translations) {
        this.translations = translations;
        return this;
    }

    public List<Exam> getTests() {
        return tests;
    }

    public Answer setTests(final List<Exam> tests) {
        this.tests = tests;
        return this;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final var answer = (Answer) object;
        return id == answer.id
                && correct == answer.correct
                && Objects.equals(translations, answer.translations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, correct, translations);
    }


    @Override
    public String toString() {
        return "Answer{" +
                "id=" + id +
                ", correct=" + correct +
                ", question=" + question.getId() +
                ", translations=" + translations +
                ", tests=" + tests +
                '}';
    }

}
