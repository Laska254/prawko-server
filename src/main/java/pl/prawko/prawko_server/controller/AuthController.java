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
import pl.prawko.prawko_server.dto.ResetPasswordRequest;
import pl.prawko.prawko_server.service.implementation.PasswordResetService;

/**
 * REST controller for authentication operations.
 *
 * <p>Provides endpoints for user login and resetting forgotten passwords.
 * Authentication is performed using Spring Security's authentication manager.
 * The API is stateless, so nothing is stored between requests.
 */
@Tag(name = "Auth", description = "Authentication management endpoints")
@RestController
@RequestMapping(ApiConstants.AUTH_BASE_URL)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;

    public AuthController(final AuthenticationManager authenticationManager,
                          final PasswordResetService passwordResetService) {
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
    }

    /**
     * Authenticates a user with provided credentials.
     *
     * <p>Only verifies the credentials. The API is stateless, so no session is created and
     * every subsequent request must carry its own HTTP Basic credentials.
     *
     * @param request the {@link LoginDto} containing username and password
     * @return a {@link ResponseEntity} with HTTP 200 Ok and success message
     */
    @Operation(summary = "Sign-in", description = "Authenticates a user with username and password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User signed-in successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication failed"),
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
     * <p>Responds the same whether or not an account with the email exists, so it can't be used to discover accounts.
     *
     * @param request the {@link ForgotPasswordRequest} containing the account's email
     * @return a {@link ResponseEntity} with HTTP 202 Accepted
     */
    @Operation(summary = "Forgot password", description = "Emails a password reset link if an account with the email exists.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Request accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid argument"),
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

}
