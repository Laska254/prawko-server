package pl.prawko.prawko_server.dto;

import org.springframework.lang.NonNull;

public record UserDto(

        long id,
        @NonNull String firstName,
        @NonNull String lastName,
        @NonNull String userName,
        @NonNull String email

) {
}
