package pl.prawko.prawko_server.model;

/**
 * How far an {@link IdentityProvider} vouches for the email of a {@link ProviderProfile}.
 */
public enum EmailStatus {

    /**
     * The provider hasn't verified that the account owns the email, or didn't share any.
     */
    UNVERIFIED,

    /**
     * The provider verified the email once, e.g. when it was added to the account, but doesn't host it, so it may have
     * changed hands since.
     */
    VERIFIED,

    /**
     * The provider hosts the email, so it's verified and still owned by the account.
     */
    AUTHORITATIVE;

    public boolean isVerified() {
        return this != UNVERIFIED;
    }

    public boolean isAuthoritative() {
        return this == AUTHORITATIVE;
    }

}
