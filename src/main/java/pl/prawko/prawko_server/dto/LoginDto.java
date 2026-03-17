package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record LoginDto(

        @NotNull(message = "{username.required}")
        @Pattern(regexp = "(?=.*\\S).{3,31}", message = "{username.pattern}")
        String userName,

        @NotNull(message = "{password.required}")
        @Pattern(regexp = "(?=.*\\S).{7,63}", message = "{password.pattern}")
        String password

) {
}
