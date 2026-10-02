package pl.prawko.prawko_server.test_data;

import org.springframework.lang.NonNull;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pl.prawko.prawko_server.dto.ChangePasswordRequest;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.dto.UserUpdateRequest;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

public class UserTestData {

    public static final String USER_ALREADY_EXISTS = "User already exists.";

    public static final Map<String, String> PIPPIN_CONFLICT_DETAILS = Map.of(
            "userName", "User with username 'pippin' already exists.",
            "email", "User with email 'pippin@shire.me' already exists.");

    private UserTestData() {
    }

    public static String userNotFoundMessage(final long id) {
        return "User with id '" + id + "' not found.";
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
                .setRole(Role.USER)
                .setEnabled(true)
                .setCreated(LocalDateTime.now())
                .setUpdated(LocalDateTime.now())
                .setExams(new ArrayList<>());
    }

    public static User createTestUserPippin() {
        return createTestUser("Peregrin", "Tuk", "pippin", "pippin@shire.me");
    }

    public static User createMerry() {
        return createTestUser("Meriadok", "Brandybuck", "merry", "merry@shire.me");
    }

    public static UserDto createUserDto(long id) {
        return new UserDto(
                id,
                "Peregrin",
                "Tuk",
                "pippin",
                "pippin@shire.me");
    }

    public static UserDto createUpdatedUserDto(long id) {
        return new UserDto(
                id,
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

    public static ChangePasswordRequest createValidChangePasswordRequest() {
        return new ChangePasswordRequest("lembasy", "racuchy");
    }

    public static ChangePasswordRequest createWrongCurrentPasswordRequest() {
        return new ChangePasswordRequest("wrongPassword", "drugieSniadanie");
    }

    public static ChangePasswordRequest createSameAsCurrentPasswordRequest() {
        return new ChangePasswordRequest("lembasy", "lembasy");
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
