package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for password change requests.
 *
 * <p>Requires the user's current password for verification and a new password to set.
 *
 */
@Schema(description = "Password change request containing current and new password")
public record ChangePasswordRequest(

        @NotBlank(message = "{currentpassword.required}")
        @Schema(description = "User's current password")
        String currentPassword,

        @NotBlank(message = "{password.required}")
        @Size(min = 7, message = "{password.size.min}")
        @Size(max = 63, message = "{password.size.max}")
        @Schema(description = "New password to set", minLength = 7, maxLength = 63)
        String newPassword

) {
}
