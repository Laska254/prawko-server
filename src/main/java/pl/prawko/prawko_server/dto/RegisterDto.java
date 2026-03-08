package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDto(

        @NotBlank(message = "First name is required.")
        @Size(min = 3, message = "First name is too short.")
        @Size(max = 31, message = "First name is too long.")
        String firstName,

        @NotBlank(message = "Last name is required.")
        @Size(min = 3, message = "Last name is too short.")
        @Size(max = 31, message = "Last name is too long.")
        String lastName,

        @NotBlank(message = "Username is required.")
        @Size(min = 3, message = "Username is too short, must be longer than 3 characters.")
        @Size(max = 31, message = "Username is too long, must be shorter than 31 characters.")
        String userName,

        @NotBlank(message = "Email is required.")
        @Size(min = 5, message = "Email is too short, must be longer than 5 characters.")
        @Size(max = 63, message = "Email is too long, must be shorter than 63 characters.")
        @Email(message = "Email format is not valid.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 7, message = "Password is too short, must be longer than 7 characters.")
        @Size(max = 63, message = "Password must be shorter than 63 characters.")
        String password

) {
}
