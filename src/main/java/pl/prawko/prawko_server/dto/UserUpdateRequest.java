package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * DTO for user update requests.
 *
 * <p>Allows partial updates to user profile information. All fields are optional;
 * only provided fields will be updated.
 *
 */
@Schema(description = "User profile update request (all fields optional)")
public record UserUpdateRequest(

        @Nullable
        @Size(min = 3, message = "{firstname.size.min}")
        @Size(max = 31, message = "{firstname.size.max}")
        @Schema(description = "User's first name", minLength = 3, maxLength = 31, nullable = true)
        String firstName,

        @Nullable
        @Size(min = 3, message = "{lastname.size.min}")
        @Size(max = 31, message = "{lastname.size.max}")
        @Schema(description = "User's last name", minLength = 3, maxLength = 31, nullable = true)
        String lastName,

        @Nullable
        @Size(min = 3, message = "{username.size.min}")
        @Size(max = 31, message = "{username.size.max}")
        @Schema(description = "Username for login", minLength = 3, maxLength = 31, nullable = true)
        String userName,

        @Nullable
        @Size(min = 5, message = "{email.size.max}")
        @Size(max = 63, message = "{email.size.max}")
        @Email(message = "{email.notvalid}")
        @Schema(description = "User's email address", minLength = 5, maxLength = 63, format = "email", nullable = true)
        String email

) {
}
