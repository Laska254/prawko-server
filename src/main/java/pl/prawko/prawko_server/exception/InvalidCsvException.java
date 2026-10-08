package pl.prawko.prawko_server.exception;

/**
 * Custom exception to throw when an uploaded CSV file has malformed content.
 */
public class InvalidCsvException extends RuntimeException {

    public InvalidCsvException(final String message) {
        super(message);
    }

}
