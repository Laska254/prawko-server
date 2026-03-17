package pl.prawko.prawko_server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.util.Objects;

/**
 * Represents a translation of a {@link Question} into a specific {@link Language}
 * <p>
 * A {@code QuestionTranslation} has:
 * <ul>
 *     <li>id - generated unique identifier</li>
 *     <li>content - text of translation</li>
 * </ul>
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link Question}: Many translations are assigned to a single question.</li>
 *     <li>{@link Language}: Many translations are assigned to a single language.</li>
 * </ul>
 * All setters are returning {@code QuestionTranslation} itself, enabling method chaining.
 */
@Entity
public class QuestionTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne
    @JoinColumn(name = "language_id")
    private Language language;

    public long getId() {
        return id;
    }

    public QuestionTranslation setId(final long id) {
        this.id = id;
        return this;
    }

    public String getContent() {
        return content;
    }

    public QuestionTranslation setContent(final String content) {
        this.content = content;
        return this;
    }

    public Question getQuestion() {
        return question;
    }

    public QuestionTranslation setQuestion(final Question question) {
        this.question = question;
        return this;
    }

    public Language getLanguage() {
        return language;
    }

    public QuestionTranslation setLanguage(final Language language) {
        this.language = language;
        return this;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final var that = (QuestionTranslation) object;
        return id == that.id
                && Objects.equals(content, that.content)
                && Objects.equals(language, that.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content, language);
    }

    @Override
    public String toString() {
        return "QuestionTranslation{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", question=" + question.getId() +
                ", language=" + language.getCode() +
                '}';
    }

}
