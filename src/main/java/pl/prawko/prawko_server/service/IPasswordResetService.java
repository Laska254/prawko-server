package pl.prawko.prawko_server.service;

import pl.prawko.prawko_server.dto.ResetPasswordRequest;
import pl.prawko.prawko_server.model.User;

/**
 * Service interface for resetting forgotten {@link User} passwords via email.
 */
public interface IPasswordResetService {

    /**
     * Issues a single-use password reset token and emails a reset link to the user.
     * <p>
     * Does nothing if no user has the given email, so callers can't tell whether an account exists.
     * A new request invalidates any previously issued token.
     * </p>
     *
     * @param email the email address of the account, case-insensitive
     */
    void requestReset(String email);

    /**
     * Sets a new password for the user owning a valid password reset token.
     * <p>
     * The token is invalidated afterward.
     * </p>
     *
     * @param request the request containing the token and new password
     */
    void resetPassword(ResetPasswordRequest request);

}
