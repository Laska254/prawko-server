package pl.prawko.prawko_server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for signing in with a Google account.
 */
@Schema(description = "Google sign-in request containing the ID token the client got from Google")
public record GoogleSignInRequest(

        @NotBlank(message = "{token.required}")
        @Schema(description = "Google ID token, issued for one of the server's configured client IDs")
        String idToken

) {
}
