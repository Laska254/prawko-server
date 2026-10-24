package pl.prawko.prawko_server.model;

/**
 * Outcome of using a valid email verification token.
 */
public enum VerificationResult {

    /**
     * The account is enabled and a pending email change, if there was one, took effect.
     */
    COMPLETED,

    /**
     * The email change is confirmed by this address and still awaits the link sent to the other one.
     */
    AWAITING_OTHER_LINK

}
