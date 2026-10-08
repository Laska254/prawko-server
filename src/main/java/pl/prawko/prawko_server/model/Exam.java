package pl.prawko.prawko_server.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a {@code Exam} entity within the application.
 * <p>
 * An {@code Exam} has:
 *  <ul>
 *     <li>{@code id} - unique generated identifier</li>
 *     <li>{@code active} - status of exam, representing if user is solving it</li>
 *     <li>{@code score} - sum of correct answers in exam by user</li>
 *     <li>{@code created} - timestamp of creation</li>
 *     <li>{@code updated} - timestamp of last update</li>
 * </ul>
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link User}: Each {@code User} can have multiple exams</li>
 *     <li>{@link Question}: Each exam contains 20 basic questions and  12 specialized questions</li>
 *     <li>{@link Answer} Each exam contains 32 user answers</li>
 *     <li>{@link Language}: Each exam have single language</li>
 *     <li>{@link Category}: Each exam have single category or multiple categories</li>
 * </ul>
 * The entity is mapped to the database table {@code exam} and uses automatic timestamp handling.
 * All setters are returning {@code Exam} itself, enabling method chaining.
 */
@Entity
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private boolean active;
    private int score;

    @CreationTimestamp
    private LocalDateTime created;

    @UpdateTimestamp
    private LocalDateTime updated;

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

    public LocalDateTime getCreated() {
        return created;
    }

    public Exam setCreated(final LocalDateTime created) {
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

    public LocalDateTime getUpdated() {
        return updated;
    }

    public Exam setUpdated(final LocalDateTime updated) {
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
                ", user=" + (user == null ? null : user.getId()) +
                ", language=" + (language == null ? null : language.getCode()) +
                ", category=" + (category == null ? null : category.getName()) +
                '}';
    }

}
