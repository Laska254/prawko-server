package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user registration requests.
 *
 * <p>Contains all required information to register a new user account in the system.
 * All fields are validated using Jakarta Validation annotations.
 *
 */
@Schema(description = "User registration request containing credentials and personal information")
public record RegisterDto(

        @NotBlank(message = "{firstname.required}")
        @Size(min = 3, message = "{firstname.size.min}")
        @Size(max = 31, message = "{firstname.size.max}")
        @Schema(description = "User's first name", minLength = 3, maxLength = 31)
        String firstName,

        @NotBlank(message = "{lastname.required}")
        @Size(min = 3, message = "lastname.size.min")
        @Size(max = 31, message = "lastname.size.max")
        @Schema(description = "User's last name", minLength = 3, maxLength = 31)
        String lastName,

        @NotBlank(message = "{username.required}")
        @Size(min = 3, message = "{username.size.min}")
        @Size(max = 31, message = "{username.size.max}")
        @Schema(description = "Username for login", minLength = 3, maxLength = 31)
        String userName,

        @NotBlank(message = "{email.required}")
        @Size(min = 5, message = "{email.size.min}")
        @Size(max = 63, message = "{email.size.max}")
        @Email(message = "{email.notvalid}")
        @Schema(description = "User's email address", minLength = 5, maxLength = 63, format = "email")
        String email,

        @NotBlank(message = "{password.required}")
        @Size(min = 7, message = "{password.size.min}")
        @Size(max = 63, message = "{password.size.max}")
        @Schema(description = "User's password for authentication", minLength = 7, maxLength = 63)
        String password

) {
}
