package pl.prawko.prawko_server.dto;

public record UserDto(

        long id,
        String firstName,
        String lastName,
        String userName,
        String email

) {
}
