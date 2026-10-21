package pl.prawko.prawko_server.service;

import org.jspecify.annotations.Nullable;
import pl.prawko.prawko_server.model.User;

/**
 * Service interface for verifying email addresses of {@link User}s.
 * <p>
 * A new account is disabled, so it can't sign in or use the API, until the link emailed to its address is opened.
 * A changed email address is pending until the link emailed to the new address is opened. When users change it
 * themselves, also the link emailed to the current address must be opened, so a stolen password alone isn't enough
 * to take over the account by moving its email.
 */
public interface IEmailVerificationService {

    /**
     * Issues a single-use email verification token for the user, replacing any previous one.
     * <p>
     * Only the token's hash is set on the user, the caller persists it. The returned token is meant for
     * {@link #sendVerificationEmail} or {@link #sendEmailChangeEmails}.
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
     * {@link #sendEmailChangeEmails}.
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
     * so a change the owner didn't request never takes effect. Without a current email token, the current address
     * is only notified that an administrator requested the change.
     * <p>
     * Like {@link #sendVerificationEmail}, returns without waiting for the emails to be sent.
     * </p>
     *
     * @param userId            the ID of the user, used for logging
     * @param currentEmail      the user's current email address
     * @param newEmail          the requested email address to verify
     * @param token             the token issued by {@link #issueToken}
     * @param currentEmailToken the token issued by {@link #issueCurrentEmailToken}, or {@code null} if the current
     *                          address needn't confirm the change
     */
    void sendEmailChangeEmails(long userId,
                               String currentEmail,
                               String newEmail,
                               String token,
                               @Nullable String currentEmailToken);

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
     * The link goes to the user's pending email, if there is one.
     * </p>
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
     */
    void verify(String token);

    /**
     * Deletes users who didn't verify their email address before their verification token expired,
     * so they don't hold their username and email forever.
     * <p>
     * Runs periodically, every {@code email-verification.cleanup-interval}.
     * </p>
     */
    void deleteExpiredUnverifiedUsers();

}
