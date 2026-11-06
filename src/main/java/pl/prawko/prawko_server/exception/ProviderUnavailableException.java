package pl.prawko.prawko_server.exception;

/**
 * Custom exception to throw when a credential issued by an identity provider can't be verified because the provider
 * can't be reached, e.g. its signing keys can't be fetched.
 */
public class ProviderUnavailableException extends RuntimeException {

    public ProviderUnavailableException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
