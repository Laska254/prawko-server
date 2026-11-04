package pl.prawko.prawko_server.exception;

/**
 * Custom exception to throw when signing in with an identity provider that isn't configured on this server.
 */
public class ProviderNotConfiguredException extends RuntimeException {

    public ProviderNotConfiguredException(final String message) {
        super(message);
    }

}
