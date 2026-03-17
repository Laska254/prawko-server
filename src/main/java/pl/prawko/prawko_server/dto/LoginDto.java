package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record LoginDto(

        @NotNull(message = "{login.username.notnull}")
        @Pattern(regexp = "(?=.*\\S).{3,31}", message = "{login.username.pattern}")
        String userName,

        @NotNull(message = "{login.password.notnull}")
        @Pattern(regexp = "(?=.*\\S).{7,63}", message = "{login.password.pattern}")
        String password

) {
}
