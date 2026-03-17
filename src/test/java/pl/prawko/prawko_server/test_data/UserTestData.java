package pl.prawko.prawko_server.test_data;

import org.springframework.lang.NonNull;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.dto.UserUpdateRequest;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserTestData {

    private UserTestData() {
    }

    public static User createTestUser(@NonNull final String firstName,
                                      @NonNull final String lastName,
                                      @NonNull final String userName,
                                      @NonNull final String email) {
        return new User()
                .setFirstName(firstName)
                .setLastName(lastName)
                .setUserName(userName)
                .setEmail(email)
                .setPassword(new BCryptPasswordEncoder().encode("lembasy"))
                .setRoles(List.of(new Role().setName("USER")))
                .setEnabled(true)
                .setCreated(LocalDateTime.now())
                .setUpdated(LocalDateTime.now())
                .setExams(new ArrayList<>());
    }

    public static User createTestUserPippin() {
        return createTestUser("Peregrin", "Tuk", "pippin", "pippin@shire.me");
    }

    public static UserDto createUserDto() {
        return new UserDto(
                1L,
                "Peregrin",
                "Tuk",
                "pippin",
                "pippin@shire.me");
    }

    public static UserDto createUpdatedUserDto() {
        return new UserDto(
                1L,
                "UpdatedFirstName",
                "UpdatedLastName",
                "UpdatedUserName",
                "UpdatedEmail@shire.me"
        );
    }

    public static UserUpdateRequest createValidUserUpdateRequest() {
        return new UserUpdateRequest(
                "UpdatedFirstName",
                "UpdatedLastName",
                "UpdatedUserName",
                "UpdatedEmail@shire.me");
    }

    public static UserUpdateRequest createInvalidUserUpdateRequest() {
        return new UserUpdateRequest("", "", "gimli", "gimli.shire.me");
    }

    public static RegisterDto createValidRegisterDto() {
        return new RegisterDto(
                "Peregrin",
                "Tuk",
                "pippin",
                "pippin@shire.me",
                "lembasy");
    }

}
