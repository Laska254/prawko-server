package pl.prawko.prawko_server.exception;

/**
 * Custom exception to throw when an imported question's correct answer isn't the label of any answer, so its type
 * can't be told.
 */
public class UnknownCorrectAnswerException extends RuntimeException {

    private final char label;

    public UnknownCorrectAnswerException(final char label) {
        super("Unknown correct answer: " + label);
        this.label = label;
    }

    public char getLabel() {
        return label;
    }

}
