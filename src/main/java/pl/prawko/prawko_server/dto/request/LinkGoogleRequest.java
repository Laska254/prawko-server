package pl.prawko.prawko_server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

/**
 * DTO for linking a Google account to the currently authenticated user.
 *
 * <p>Requires re-entering the current password if the user has one, its absence is reported like a wrong password.
 */
@Schema(description = "Request to link a Google account, containing the ID token the client got from Google")
public record LinkGoogleRequest(

        @NotBlank(message = "{token.required}")
        @Schema(description = "Google ID token, issued for one of the server's configured client IDs")
        String idToken,

        @Nullable
        @Schema(description = "User's current password, required if they have one", nullable = true)
        String currentPassword

) {
}
