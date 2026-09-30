package pl.prawko.prawko_server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;


/**
 * Represents a {@code AnswerTranslation} entity within the application.
 * <p>
 * An {@code AnswerTranslation} has:
 *  <ul>
 *     <li>{@code id} - generated unique identifier</li>
 *     <li>{@code content} - content of {@link Answer} in specific language</li>
 * </ul>
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link Answer}: Multiple translations can be assigned to a single answer</li>
 *     <li>{@link Language}: Multiple translations can be assigned to a single language</li>
 * </ul>
 * <p>
 * The entity is mapped to the database table {@code answer_translation}.
 * All setters are returning {@code AnswerTranslation} itself, enabling method chaining.
 */
@Entity
public class AnswerTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "answer_id")
    private Answer answer;

    @ManyToOne
    @JoinColumn(name = "language_id")
    private Language language;

    public AnswerTranslation() {
        this(null, null, null);
    }

    public AnswerTranslation(final Answer answer,
                             final Language language,
                             final String content) {
        this.content = content;
        this.answer = answer;
        this.language = language;
    }

    public long getId() {
        return id;
    }

    public AnswerTranslation setId(final long id) {
        this.id = id;
        return this;
    }

    public String getContent() {
        return content;
    }

    public AnswerTranslation setContent(final String content) {
        this.content = content;
        return this;
    }

    public Answer getAnswer() {
        return answer;
    }

    public AnswerTranslation setAnswer(final Answer answer) {
        this.answer = answer;
        return this;
    }

    public Language getLanguage() {
        return language;
    }

    public AnswerTranslation setLanguage(final Language language) {
        this.language = language;
        return this;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof AnswerTranslation other)) {
            return false;
        }
        return id != 0 && id == other.getId();
    }

    @Override
    public int hashCode() {
        return AnswerTranslation.class.hashCode();
    }

    @Override
    public String toString() {
        return "AnswerTranslation{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", answer=" + (answer == null ? null : answer.getId()) +
                ", language=" + (language == null ? null : language.getCode()) +
                '}';
    }

}
