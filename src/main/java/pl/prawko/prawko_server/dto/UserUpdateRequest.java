package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

public record UserUpdateRequest(

        @Nullable
        @Size(min = 3, message = "{userupdate.firstname.size.min}")
        @Size(max = 31, message = "{userupdate.firstname.size.max}")
        String firstName,

        @Nullable
        @Size(min = 3, message = "{userupdate.lastname.size.min}")
        @Size(max = 31, message = "{userupdate.lastname.size.max}")
        String lastName,

        @Nullable
        @Size(min = 3, message = "{userupdate.username.size.min}")
        @Size(max = 31, message = "{userupdate.username.size.max}")
        String userName,

        @Nullable
        @Size(min = 5, message = "{userupdate.email.size.max}")
        @Size(max = 63, message = "{userupdate.email.size.max}")
        @Email(message = "{userupdate.email.notvalid}")
        String email

) {
}
