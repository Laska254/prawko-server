package pl.prawko.prawko_server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.ForgotPasswordRequest;
import pl.prawko.prawko_server.dto.LoginDto;
import pl.prawko.prawko_server.dto.ResendVerificationRequest;
import pl.prawko.prawko_server.dto.ResetPasswordRequest;
import pl.prawko.prawko_server.dto.VerifyEmailRequest;
import pl.prawko.prawko_server.service.implementation.EmailVerificationService;
import pl.prawko.prawko_server.service.implementation.PasswordResetService;

/**
 * REST controller for authentication operations.
 *
 * <p>Provides endpoints for user login, verifying email addresses and resetting forgotten passwords.
 * Authentication is performed using Spring Security's authentication manager.
 * The API is stateless, so nothing is stored between requests.
 */
@Tag(name = "Auth", description = "Authentication management endpoints")
@RestController
@RequestMapping(ApiConstants.AUTH_BASE_URL)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(final AuthenticationManager authenticationManager,
                          final PasswordResetService passwordResetService,
                          final EmailVerificationService emailVerificationService) {
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * Authenticates a user with provided credentials.
     *
     * <p>Only verifies the credentials. The API is stateless, so no session is created and
     * every subsequent request must carry its own HTTP Basic credentials. Accounts whose email isn't verified yet
     * are rejected.
     *
     * @param request the {@link LoginDto} containing username and password
     * @return a {@link ResponseEntity} with HTTP 200 Ok and success message
     */
    @Operation(summary = "Sign-in", description = "Authenticates a user with username and password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User signed-in successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication failed"),
            @ApiResponse(responseCode = "403", description = "Email address is not verified"),
            @ApiResponse(responseCode = "400", description = "Invalid argument"),
    })
    @PostMapping
    public ResponseEntity<String> login(@Valid @RequestBody final LoginDto request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.userName(),
                request.password()
        ));
        return ResponseEntity.ok("User signed-in successfully.");
    }

    /**
     * Requests a password reset link to be emailed.
     *
     * <p>Responds the same and without waiting for the email to be sent, whether or not an account with the email
     * exists, so it can't be used to discover accounts.
     *
     * @param request the {@link ForgotPasswordRequest} containing the account's email
     * @return a {@link ResponseEntity} with HTTP 202 Accepted
     */
    @Operation(summary = "Forgot password", description = "Emails a password reset link if an account with the email exists.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Request accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid argument"),
            @ApiResponse(responseCode = "503", description = "Too many reset requests pending"),
    })
    @PostMapping(ApiConstants.FORGOT_PASSWORD)
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody final ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.accepted().build();
    }

    /**
     * Sets a new password using the token from a password reset link.
     *
     * @param request the {@link ResetPasswordRequest} containing the token and new password
     * @return a {@link ResponseEntity} with HTTP 204 No Content
     */
    @Operation(summary = "Reset password", description = "Sets a new password using a password reset token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid argument or invalid/expired token"),
    })
    @PostMapping(ApiConstants.RESET_PASSWORD)
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody final ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Verifies an email address using the token from a verification link: enables a new account or confirms
     * a pending email change, which takes effect once all its links are opened.
     *
     * @param request the {@link VerifyEmailRequest} containing the token
     * @return a {@link ResponseEntity} with HTTP 204 No Content, or HTTP 202 Accepted if an email change still awaits
     * the other link
     */
    @Operation(summary = "Verify email",
            description = "Activates an account or confirms an email change using a token from a verification link. "
                    + "An email change takes effect once confirmed by both the new and the current address.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Email verified successfully"),
            @ApiResponse(responseCode = "202",
                    description = "Email change confirmed by this address, awaiting the link sent to the other one"),
            @ApiResponse(responseCode = "400", description = "Invalid argument or invalid/expired token"),
            @ApiResponse(responseCode = "409", description = "The new email was taken by another user meanwhile"),
    })
    @PostMapping(ApiConstants.VERIFY_EMAIL)
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody final VerifyEmailRequest request) {
        return switch (emailVerificationService.verify(request.token())) {
            case COMPLETED -> ResponseEntity.noContent().build();
            case AWAITING_OTHER_LINK -> ResponseEntity.accepted().build();
        };
    }

    /**
     * Requests another email verification link to be emailed.
     *
     * <p>Responds the same and without waiting for the email to be sent, whether or not an unverified account with
     * the email exists.
     *
     * @param request the {@link ResendVerificationRequest} containing the account's email
     * @return a {@link ResponseEntity} with HTTP 202 Accepted
     */
    @Operation(summary = "Resend verification email",
            description = "Emails a new verification link if an unverified account with the email exists.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Request accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid argument"),
            @ApiResponse(responseCode = "503", description = "Too many requests pending"),
    })
    @PostMapping(ApiConstants.RESEND_VERIFICATION)
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody final ResendVerificationRequest request) {
        emailVerificationService.resendVerification(request.email());
        return ResponseEntity.accepted().build();
    }

}
