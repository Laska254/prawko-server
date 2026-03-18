package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record UserUpdateRequest(

        @Nullable
        @Size(min = 3, message = "{firstname.size.min}")
        @Size(max = 31, message = "{firstname.size.max}")
        String firstName,

        @Nullable
        @Size(min = 3, message = "{lastname.size.min}")
        @Size(max = 31, message = "{lastname.size.max}")
        String lastName,

        @Nullable
        @Size(min = 3, message = "{username.size.min}")
        @Size(max = 31, message = "{username.size.max}")
        String userName,

        @Nullable
        @Size(min = 5, message = "{email.size.max}")
        @Size(max = 63, message = "{email.size.max}")
        @Email(message = "{email.notvalid}")
        String email

) {
}
