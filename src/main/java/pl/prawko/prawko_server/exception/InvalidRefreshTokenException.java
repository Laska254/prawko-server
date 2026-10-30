package pl.prawko.prawko_server.exception;

/**
 * Custom exception to throw when provided refresh token is unknown, expired or revoked.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(final String message) {
        super(message);
    }

}
