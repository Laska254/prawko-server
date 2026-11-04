package pl.prawko.prawko_server.exception;

/**
 * Custom exception to throw when a credential issued by an identity provider fails verification, e.g. it's expired,
 * not signed by the provider or issued for another client.
 */
public class InvalidProviderCredentialException extends RuntimeException {

    public InvalidProviderCredentialException(final String message) {
        super(message);
    }

}
