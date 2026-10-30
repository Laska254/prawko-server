package pl.prawko.prawko_server.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Represents a {@code Exam} entity within the application.
 * <p>
 * An {@code Exam} has:
 *  <ul>
 *     <li>{@code id} - unique generated identifier</li>
 *     <li>{@code active} - status of exam, representing if user is solving it</li>
 *     <li>{@code score} - sum of points of questions answered correctly, set when the exam is finished; at least
 *     {@link #PASSING_SCORE} passes</li>
 *     <li>{@code created} - timestamp of creation</li>
 *     <li>{@code expires} - deadline for answers, {@link #DURATION} after creation; the exam is finished automatically afterward</li>
 *     <li>{@code updated} - timestamp of last update</li>
 *     <li>{@code version} - optimistic lock, so concurrent changes (e.g. to user answers) fail instead of overwriting each other</li>
 * </ul>
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link User}: Each {@code User} can have multiple exams</li>
 *     <li>{@link Question}: Each exam contains 20 basic questions and  12 specialized questions</li>
 *     <li>{@link Answer}: Answers submitted so far, at most one per question; a new one replaces the previous answer to that question</li>
 *     <li>{@link Language}: Each exam have single language</li>
 *     <li>{@link Category}: Each exam have single category or multiple categories</li>
 * </ul>
 * The entity is mapped to the database table {@code exam}. Timestamps are {@link Instant}s, so they don't depend on
 * the JVM's time zone; {@code created} and {@code expires} are set together when the exam is created (so
 * {@code expires} is exactly {@code created} + {@link #DURATION}), {@code updated} is set automatically.
 * All setters are returning {@code Exam} itself, enabling method chaining.
 */
@Entity
public class Exam {

    public static final Duration DURATION = Duration.ofMinutes(25);
    public static final int PASSING_SCORE = 68;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private boolean active;
    private int score;

    private Instant created;

    @UpdateTimestamp
    private Instant updated;

    private Instant expires;

    @Version
    private long version;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToMany
    @JoinTable(
            name = "test_question",
            joinColumns = @JoinColumn(name = "test_id"),
            inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private List<Question> questions;

    @ManyToMany
    @JoinTable(
            name = "test_answer",
            joinColumns = @JoinColumn(name = "test_id"),
            inverseJoinColumns = @JoinColumn(name = "answer_id")
    )
    private List<Answer> userAnswers;

    @ManyToOne
    @JoinColumn(name = "language_id")
    private Language language;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public long getId() {
        return id;
    }

    public Exam setId(final long id) {
        this.id = id;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public Exam setCreated(final Instant created) {
        this.created = created;
        return this;
    }

    public User getUser() {
        return user;
    }

    public Exam setUser(final User user) {
        this.user = user;
        return this;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public Exam setQuestions(final List<Question> questions) {
        this.questions = questions;
        return this;
    }

    public List<Answer> getUserAnswers() {
        return userAnswers;
    }

    public Exam setUserAnswers(final List<Answer> userAnswers) {
        this.userAnswers = userAnswers;
        return this;
    }

    public boolean isActive() {
        return active;
    }

    public Exam setActive(final boolean active) {
        this.active = active;
        return this;
    }

    public int getScore() {
        return score;
    }

    public Exam setScore(final int score) {
        this.score = score;
        return this;
    }

    public Language getLanguage() {
        return language;
    }

    public Exam setLanguage(final Language language) {
        this.language = language;
        return this;
    }

    public Category getCategory() {
        return category;
    }

    public Exam setCategory(final Category category) {
        this.category = category;
        return this;
    }

    public Instant getExpires() {
        return expires;
    }

    public Exam setExpires(final Instant expires) {
        this.expires = expires;
        return this;
    }

    public Instant getUpdated() {
        return updated;
    }

    public Exam setUpdated(final Instant updated) {
        this.updated = updated;
        return this;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Exam other)) {
            return false;
        }
        return id != 0 && id == other.getId();
    }

    @Override
    public int hashCode() {
        return Exam.class.hashCode();
    }

    @Override
    public String toString() {
        return "Exam{" +
                "id=" + id +
                ", active=" + active +
                ", score=" + score +
                ", created=" + created +
                ", updated=" + updated +
                ", expires=" + expires +
                ", user=" + (user == null ? null : user.getId()) +
                ", language=" + (language == null ? null : language.getCode()) +
                ", category=" + (category == null ? null : category.getName()) +
                '}';
    }

}
