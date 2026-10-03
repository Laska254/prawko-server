package pl.prawko.prawko_server.exception;

/**
 * Custom exception to throw when provided token is unknown or expired.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(final String message) {
        super(message);
    }

}
