package pl.prawko.prawko_server.service;

import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.model.VerificationResult;

/**
 * Service interface for verifying email addresses of {@link User}s.
 * <p>
 * A new account is disabled, so it can't sign in or use the API, until the link emailed to its address is opened.
 * A changed email address is pending until the links emailed to the new and the current address are opened, so
 * a stolen password alone isn't enough to take over the account by moving its email.
 */
public interface IEmailVerificationService {

    /**
     * Issues a single-use email verification token for the user, replacing any previous one.
     * <p>
     * Only the token's hash is set on the user, the caller persists it. The returned token is meant for
     * {@link #sendVerificationEmail} or {@link #sendEmailChangeConfirmation}.
     * </p>
     *
     * @param user the user with an unverified email address
     * @return the issued token
     */
    String issueToken(User user);

    /**
     * Issues a single-use token for the user's current address to confirm a pending email change, replacing any
     * previous one.
     * <p>
     * It shares the expiry of the token issued by {@link #issueToken}, so it's meant to be issued along with it.
     * Only the token's hash is set on the user, the caller persists it. The returned token is meant for
     * {@link #sendEmailChangeConfirmation}.
     * </p>
     *
     * @param user the user with a pending email
     * @return the issued token
     */
    String issueCurrentEmailToken(User user);

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

    /**
     * Emails a verification link with the token to the new address and a confirmation link to the current address,
     * so a change the owner didn't request never takes effect.
     * <p>
     * Like {@link #sendVerificationEmail}, returns without waiting for the emails to be sent.
     * </p>
     *
     * @param userId            the ID of the user, used for logging
     * @param currentEmail      the user's current email address
     * @param newEmail          the requested email address to verify
     * @param token             the token issued by {@link #issueToken}
     * @param currentEmailToken the token issued by {@link #issueCurrentEmailToken}
     */
    void sendEmailChangeConfirmation(long userId,
                                     String currentEmail,
                                     String newEmail,
                                     String token,
                                     String currentEmailToken);

    /**
     * Tells whether the user's verification token was issued within the cooldown, so no new one should be sent yet.
     *
     * @param user the user to check
     * @return {@code true} if another verification email can't be sent yet
     */
    boolean isCoolingDown(User user);

    /**
     * Issues a new verification token and emails a verification link to the unverified user with the given email.
     * <p>
     * Does nothing if no unverified user has the email. Returns without waiting for the outcome.
     * Requests made within a cooldown after issuing a token are ignored, so the user can't be flooded with emails.
     * </p>
     *
     * @param email the email address of the account, case-insensitive
     */
    void resendVerification(String email);

    /**
     * Enables the user owning a valid email verification token and makes their pending email, if there is one,
     * the user's email.
     * <p>
     * Accepts the token sent to the current address to confirm an email change too. A pending email change takes
     * effect only once all its tokens are used, until then each used token only counts as a confirmation.
     * </p>
     * <p>
     * The token is invalidated afterward. A changed email invalidates any pending password reset token.
     * </p>
     *
     * @param token the token from the verification link
     * @return {@link VerificationResult#COMPLETED}, or {@link VerificationResult#AWAITING_OTHER_LINK} if the email
     * change still awaits confirmation by the other address
     */
    VerificationResult verify(String token);

    /**
     * Deletes users who didn't verify their email address before their verification token expired,
     * so they don't hold their username and email forever.
     * <p>
     * Runs periodically, every {@code email-verification.cleanup-interval}.
     * </p>
     */
    void deleteExpiredUnverifiedUsers();

}
