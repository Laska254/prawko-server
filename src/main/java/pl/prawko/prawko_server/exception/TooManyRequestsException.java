package pl.prawko.prawko_server.exception;

/**
 * Custom exception to throw when an action is requested again within its cooldown.
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(final String message) {
        super(message);
    }

}
