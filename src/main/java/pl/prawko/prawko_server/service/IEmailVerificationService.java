package pl.prawko.prawko_server.service;

import pl.prawko.prawko_server.model.User;

/**
 * Service interface for verifying email addresses of newly registered {@link User}s.
 * <p>
 * A new account is disabled, so it can't sign in or use the API, until the link emailed to its address is opened.
 */
public interface IEmailVerificationService {

    /**
     * Issues a single-use email verification token for the user, replacing any previous one.
     * <p>
     * Only the token's hash is set on the user, the caller persists it. The returned token is meant for
     * {@link #sendVerificationEmail}.
     * </p>
     *
     * @param user the unverified user
     * @return the issued token
     */
    String issueToken(User user);

    /**
     * Emails a verification link with the token to the user.
     * <p>
     * Returns without waiting for the email to be sent, so it can be called inside the transaction that persists
     * the token: if no background task can be queued, the transaction is rolled back.
     * </p>
     *
     * @param userId the ID of the user, used for logging
     * @param email  the email address to verify
     * @param token  the token issued by {@link #issueToken}
     */
    void sendVerificationEmail(long userId, String email, String token);

}
