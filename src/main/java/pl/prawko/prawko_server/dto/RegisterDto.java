package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDto(

        @NotBlank(message = "{register.firstname.notblank}")
        @Size(min = 3, message = "{register.firstname.size.min}")
        @Size(max = 31, message = "{register.firstname.size.max}")
        String firstName,

        @NotBlank(message = "{register.lastname.notblank}")
        @Size(min = 3, message = "register.lastname.size.min")
        @Size(max = 31, message = "register.lastname.size.max")
        String lastName,

        @NotBlank(message = "{register.username.notblank}")
        @Size(min = 3, message = "{register.username.size.min}")
        @Size(max = 31, message = "{register.username.size.max}")
        String userName,

        @NotBlank(message = "{register.email.notblank}")
        @Size(min = 5, message = "{register.email.size.min}")
        @Size(max = 63, message = "{register.email.size.max}")
        @Email(message = "{register.email.notvalid}")
        String email,

        @NotBlank(message = "{register.password.notblank}")
        @Size(min = 7, message = "{register.password.size.min}")
        @Size(max = 63, message = "{register.password.size.max}")
        String password

) {
}
