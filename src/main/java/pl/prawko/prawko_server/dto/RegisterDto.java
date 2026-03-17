package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDto(

        @NotBlank(message = "{firstname.required}")
        @Size(min = 3, message = "{firstname.size.min}")
        @Size(max = 31, message = "{firstname.size.max}")
        String firstName,

        @NotBlank(message = "{lastname.required}")
        @Size(min = 3, message = "lastname.size.min")
        @Size(max = 31, message = "lastname.size.max")
        String lastName,

        @NotBlank(message = "{username.required}")
        @Size(min = 3, message = "{username.size.min}")
        @Size(max = 31, message = "{username.size.max}")
        String userName,

        @NotBlank(message = "{email.required}")
        @Size(min = 5, message = "{email.size.min}")
        @Size(max = 63, message = "{email.size.max}")
        @Email(message = "{email.notvalid}")
        String email,

        @NotBlank(message = "{password.required}")
        @Size(min = 7, message = "{password.size.min}")
        @Size(max = 63, message = "{password.size.max}")
        String password

) {
}
