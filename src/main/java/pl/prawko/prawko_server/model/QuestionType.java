package pl.prawko.prawko_server.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the type of question.
 * <p>
 * Each type is associated with a descriptive name in Polish because that's how it's stored in the government's question
 * catalogue from <a href="https://www.gov.pl/web/infrastruktura/prawo-jazdy">Officials questions for the Polish
 * driving licence test</a>, with the labels its correct answer can have, which decide the type on import (see
 * {@link #ofCorrectAnswer(char)}), and with whether its answers are translated.
 */
public enum QuestionType {

    /**
     * True/False question type
     */
    BASIC("PODSTAWOWY", "TN", false, Map.ofEntries(
            Map.entry(1, 4),
            Map.entry(2, 6),
            Map.entry(3, 10))),
    /**
     * ABC question type
     */
    SPECIAL("SPECJALISTYCZNY", "ABC", true, Map.ofEntries(
            Map.entry(1, 2),
            Map.entry(2, 4),
            Map.entry(3, 6)));

    private final String name;
    private final String answerLabels;
    private final boolean translatedAnswers;
    private final Map<Integer, Integer> distribution;

    QuestionType(final String name,
                 final String answerLabels,
                 final boolean translatedAnswers,
                 final Map<Integer, Integer> distribution) {
        this.name = name;
        this.answerLabels = answerLabels;
        this.translatedAnswers = translatedAnswers;
        this.distribution = distribution;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns how many questions of this type an exam has, by points value.
     */
    public Map<Integer, Integer> getDistribution() {
        return distribution;
    }

    /**
     * Returns the labels of this type's answers, in answer order: {@code TN} (yes, no) or {@code ABC}.
     */
    public String getAnswerLabels() {
        return answerLabels;
    }

    /**
     * Returns whether this type's answers have a text in every language; yes/no answers of {@link #BASIC} have none.
     */
    public boolean hasTranslatedAnswers() {
        return translatedAnswers;
    }

    /**
     * Returns the type whose answers include the given label: {@code T} (yes) or {@code N} (no) for {@link #BASIC},
     * {@code A}, {@code B} or {@code C} for {@link #SPECIAL}.
     *
     * @param label the label of the correct answer, case-sensitive
     * @return the matching type, or empty if no type has an answer with this label
     */
    public static Optional<QuestionType> ofCorrectAnswer(final char label) {
        return Arrays.stream(values())
                .filter(type -> type.answerLabels.indexOf(label) >= 0)
                .findFirst();
    }

}
