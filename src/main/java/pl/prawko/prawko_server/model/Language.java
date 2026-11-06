package pl.prawko.prawko_server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.List;

/**
 * Represents a {@code Language} entity within the application.
 * <p>
 * A {@code Language} has:
 *  <ul>
 *     <li>{@code id} - unique generated identifier</li>
 *     <li>{@code name} - limited to 31 characters</li>
 *     <li>{@code code} - two-letter language code, e.g. {@link #PL}</li>
 *     <li>{@code icon} - limited to 3 characters</li>
 * </ul>
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link QuestionTranslation}: Each language is assigned to multiple question translations.</li>
 *     <li>{@link AnswerTranslation}: Each language is assigned to multiple answer translations.</li>
 * </ul>
 * <p>
 * The entity is mapped to the database table {@code language}.
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

    @OneToMany(mappedBy = "language")
    private List<QuestionTranslation> questionTranslations;

    @OneToMany(mappedBy = "language")
    private List<AnswerTranslation> answerTranslations;

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

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Language other)) {
            return false;
        }
        return id != 0 && id == other.getId();
    }

    @Override
    public int hashCode() {
        return Language.class.hashCode();
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
