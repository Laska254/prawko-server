package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for requesting a password reset link.
 *
 * <p>The link is sent to the given email address if an account with it exists.
 *
 */
@Schema(description = "Password reset request containing the account's email address")
public record ForgotPasswordRequest(

        @NotBlank(message = "{email.required}")
        @Size(max = 63, message = "{email.size.max}")
        @Email(message = "{email.notvalid}")
        @Schema(description = "Email address of the account", maxLength = 63, format = "email")
        String email

) {
}
