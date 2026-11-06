package pl.prawko.prawko_server.exception;

/**
 * Custom exception to throw when a provider hasn't verified the email of an account, which the user has to do at the
 * provider, as opposed to an unverified user of this app, who opens the link emailed to them.
 */
public class ProviderEmailNotVerifiedException extends RuntimeException {

    public ProviderEmailNotVerifiedException(final String message) {
        super(message);
    }

}
