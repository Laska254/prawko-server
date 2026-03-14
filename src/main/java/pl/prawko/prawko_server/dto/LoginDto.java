package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDto(

        @NotBlank(message = "Username is required.")
        String userName,

        @NotBlank(message = "Password is required.")
        String password

) {
}
