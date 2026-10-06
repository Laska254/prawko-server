package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for setting a new password with a password reset token.
 *
 * <p>The token comes from the link sent by email after a password reset request.
 *
 */
@Schema(description = "Password reset containing the emailed token and the new password")
public record ResetPasswordRequest(

        @NotBlank(message = "{token.required}")
        @Schema(description = "Password reset token from the emailed link")
        String token,

        @NotBlank(message = "{password.required}")
        @Size(min = 7, message = "{password.size.min}")
        @Size(max = 63, message = "{password.size.max}")
        @Schema(description = "New password to set", minLength = 7, maxLength = 63)
        String newPassword

) {
}
