package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

public record UserUpdateRequest(

        @Nullable
        @Size(min = 3, message = "First name is too short.")
        @Size(max = 31, message = "First name is too long.")
        String firstName,

        @Nullable
        @Size(min = 3, message = "Last name is too short.")
        @Size(max = 31, message = "Last name is too long.")
        String lastName,

        @Nullable
        @Size(min = 3, message = "Username is too short, must be longer than 3 characters.")
        @Size(max = 31, message = "Username is too long, must be shorter than 31 characters.")
        String userName,

        @Nullable
        @Size(min = 5, message = "Email is too short, must be longer than 5 characters.")
        @Size(max = 63, message = "Email is too long, must be shorter than 63 characters.")
        @Email(message = "Email format is not valid.")
        String email

) {
}
