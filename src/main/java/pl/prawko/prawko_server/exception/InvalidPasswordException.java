package pl.prawko.prawko_server.exception;

/**
 * Custom exception to throw when provided password is invalid for requested operation.
 */
public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException(final String message) {
        super(message);
    }

}
