package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for verifying an email address with an email verification token.
 *
 * <p>The token comes from the link sent by email after registration.
 *
 */
@Schema(description = "Email verification containing the emailed token")
public record VerifyEmailRequest(

        @NotBlank(message = "{token.required}")
        @Schema(description = "Email verification token from the emailed link")
        String token

) {
}
