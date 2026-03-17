package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record LoginDto(

        @NotNull(message = "Username is required.")
        @Pattern(regexp = "(?=.*\\S).{3,31}", message = "Username must not be blank and between 3 and 31 characters.")
        String userName,

        @NotNull(message = "Password is required.")
        @Pattern(regexp = "(?=.*\\S).{7,63}", message = "Password must not be blank and between 7 and 63 characters.")
        String password

) {
}
