package pl.prawko.prawko_server.exception;

/**
 * Custom exception to throw when an exam that is no longer active is about to be modified.
 */
public class ExamFinishedException extends RuntimeException {

    public ExamFinishedException(final String message) {
        super(message);
    }

}
