package pl.prawko.prawko_server.exception;

/**
 * Custom exception to throw when unlinking a provider account would leave a user with no way to sign in.
 */
public class LastSignInMethodException extends RuntimeException {

    public LastSignInMethodException(final String message) {
        super(message);
    }

}
