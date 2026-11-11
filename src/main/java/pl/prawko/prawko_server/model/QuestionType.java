package pl.prawko.prawko_server.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the type of question.
 * <p>
 * Each type is associated with a descriptive name in Polish because that's how it's stored in original CSV file from
 * <a href="https://www.gov.pl/web/infrastruktura/prawo-jazdy">Officials questions for the Polish driving licence
 * test</a>, and with the labels its correct answer can have, which decide the type on import (see
 * {@link #ofCorrectAnswer(char)}).
 */
public enum QuestionType {

    /**
     * key - points value of questions
     * value - amount of questions
     */

    /**
     * True/False question type
     */
    BASIC("PODSTAWOWY", "TN", Map.ofEntries(
            Map.entry(1, 4),
            Map.entry(2, 6),
            Map.entry(3, 10))),
    /**
     * ABC question type
     * //
     */
    SPECIAL("SPECJALISTYCZNY", "ABC", Map.ofEntries(
            Map.entry(1, 2),
            Map.entry(2, 4),
            Map.entry(3, 6)));

    private final String name;
    private final String answerLabels;
    private final Map<Integer, Integer> distribution;

    QuestionType(final String name, final String answerLabels, final Map<Integer, Integer> distribution) {
        this.name = name;
        this.answerLabels = answerLabels;
        this.distribution = distribution;
    }

    public String getName() {
        return name;
    }

    public Map<Integer, Integer> getDistribution() {
        return distribution;
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
