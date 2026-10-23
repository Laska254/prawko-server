package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.prawko.prawko_server.model.Role;

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
        String email,

        @Schema(description = "New email address awaiting verification, null if no change is pending or its link expired")
        String pendingEmail,

        @Schema(description = "Whether the link emailed to the pending email still has to be opened")
        boolean awaitingNewEmailConfirmation,

        @Schema(description = "Whether the link emailed to the current email still has to be opened to confirm the "
                + "change")
        boolean awaitingCurrentEmailConfirmation,

        @Schema(description = "User's role")
        Role role

) {
}
