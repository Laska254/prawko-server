package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for user login requests.
 *
 * <p>Contains credentials required to authenticate a user. Username can be either
 * an actual username or an email address associated with the user account.
 *
 */
@Schema(description = "User login credentials")
public record LoginDto(

        @NotNull(message = "{username.required}")
        @Pattern(regexp = "(?=.*\\S).{3,63}", message = "{login.pattern}")
        @Schema(description = "Username or email address for login", minLength = 3, maxLength = 63)
        String userName,

        @NotNull(message = "{password.required}")
        @Pattern(regexp = "(?=.*\\S).{7,63}", message = "{password.pattern}")
        @Schema(description = "User's password")
        String password

) {
}
