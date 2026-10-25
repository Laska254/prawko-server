package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for deleting the currently authenticated user's account.
 *
 * <p>Requires re-entering the current password to confirm the deletion.
 */
@Schema(description = "Account deletion request containing the current password")
public record DeleteAccountRequest(

        @NotBlank(message = "{password.required}")
        @Schema(description = "User's current password")
        String password

) {
}
