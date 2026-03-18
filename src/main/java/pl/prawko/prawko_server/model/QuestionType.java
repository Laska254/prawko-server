package pl.prawko.prawko_server.model;

import java.util.Arrays;
import java.util.Map;

/**
 * Represents the type of question.
 * <p>
 * Each type is associated with a descriptive name in Polish because that's how it's stored in original CSV file from
 * <a href="https://www.gov.pl/web/infrastruktura/prawo-jazdy">Officials questions for the Polish driving licence test</a>.
 */
public enum QuestionType {

    /**
     * key - points value of questions
     * value - amount of questions
     */

    /**
     * True/False question type
     */
    BASIC("PODSTAWOWY", Map.ofEntries(
            Map.entry(1, 4),
            Map.entry(2, 6),
            Map.entry(3, 10))),
    /**
     * ABC question type
     * //
     */
    SPECIAL("SPECJALISTYCZNY", Map.ofEntries(
            Map.entry(1, 2),
            Map.entry(2, 4),
            Map.entry(3, 6)));

    private final String name;
    private final Map<Integer, Integer> distribution;

    QuestionType(final String name, final Map<Integer, Integer> distribution) {
        this.name = name;
        this.distribution = distribution;
    }

    public String getName() {
        return name;
    }

    public Map<Integer, Integer> getDistribution() {
        return distribution;
    }

    /**
     * Returns the corresponding {@code QuestionType} based on the given name.
     *
     * @param name the name of the question type
     * @return the matching {@code QuestionType}
     * @throws IllegalStateException if the given name does not correspond to any known type
     */
    public static QuestionType ofType(final String name) {
        return Arrays.stream(QuestionType.values())
                .filter(type -> type.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unexpected value: " + name));
    }

}
