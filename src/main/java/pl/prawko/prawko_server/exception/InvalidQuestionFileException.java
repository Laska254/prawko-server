package pl.prawko.prawko_server.exception;

/**
 * Custom exception to throw when an uploaded question file is invalid: a CSV file has malformed content, or an XLSX
 * file can't be converted to CSV.
 */
public class InvalidQuestionFileException extends RuntimeException {

    public InvalidQuestionFileException(final String message) {
        super(message);
    }

}
