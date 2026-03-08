package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

public record UserUpdateRequest(

        @Nullable
        @Size(max = 31, message = "First name is too long.")
        String firstName,

        @Nullable
        @Size(max = 31, message = "Last name is too long.")
        String lastName,

        @Nullable
        @Size(min = 3, max = 31, message = "Username must be between 3 and 31 characters.")
        String userName,

        @Nullable
        @Size(max = 63, message = "Email is too long.")
        @Email(message = "Email should be valid.")
        String email

) {
}
