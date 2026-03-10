package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginDto(

        @NotBlank(message = "Username is required.")
        @Size(min = 3, message = "Username is too short, must be longer than 3 characters.")
        @Size(max = 31, message = "Username is too long, must be shorter than 31 characters.")
        String userName,

        @NotBlank(message = "Password is required.")
        @Size(min = 7, message = "Password is too short, must be longer than 7 characters.")
        @Size(max = 63, message = "Password is too long, must be shorter than 63 characters.")
        String password

) {
}
