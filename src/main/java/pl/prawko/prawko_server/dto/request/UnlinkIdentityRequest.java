package pl.prawko.prawko_server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * DTO for unlinking a provider account from the currently authenticated user.
 *
 * <p>Requires re-entering the current password if the user has one, its absence is reported like a wrong password.
 */
@Schema(description = "Request to unlink a provider account")
public record UnlinkIdentityRequest(

        @Nullable
        @Schema(description = "User's current password, required if they have one", nullable = true)
        String currentPassword

) {
}
