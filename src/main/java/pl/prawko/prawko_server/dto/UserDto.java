package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for user responses.
 *
 * <p>Contains publicly safe user information. Sensitive data like password is excluded.
 *
 */
@Schema(description = "User information response (excluding sensitive data)")
public record UserDto(

        @Schema(description = "Unique user identifier")
        long id,

        @Schema(description = "User's first name")
        String firstName,

        @Schema(description = "User's last name")
        String lastName,

        @Schema(description = "Username for login")
        String userName,

        @Schema(description = "User's email address")
        String email

) {
}
