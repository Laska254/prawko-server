package pl.prawko.prawko_server.test_data;

import org.springframework.lang.NonNull;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.dto.ChangePasswordRequest;
import pl.prawko.prawko_server.dto.DeleteAccountRequest;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.ResetPasswordRequest;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.dto.UserUpdateRequest;
import pl.prawko.prawko_server.model.EmailStatus;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.model.ProviderProfile;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.model.User;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class UserTestData {

    public static final String USER_ALREADY_EXISTS = "User already exists.";

    public static final Map<String, String> PIPPIN_CONFLICT_DETAILS = Map.ofEntries(
            Map.entry("userName", userNameConflictMessage("pippin")),
            Map.entry("email", emailConflictMessage("pippin@shire.me")));
    public static final String USER_NAME_TAKEN = "User with this username already exists.";
    public static final String EMAIL_TAKEN = "User with this email already exists.";

    public static final String INVALID_RESET_TOKEN = "Password reset token is invalid or expired.";
    public static final String INVALID_VERIFICATION_TOKEN = "Email verification token is invalid or expired.";
    public static final String EMAIL_NOT_VERIFIED = "Email address is not verified.";
    public static final String VERIFICATION_COOLDOWN = "Verification email was sent recently, try again later.";
    public static final String INCORRECT_PASSWORD = "Current password is incorrect.";
    public static final String WRONG_PASSWORD = "wrongPassword";
    public static final String LAST_SIGN_IN_METHOD = "Can't unlink the only way to sign in. Set a password or link another "
            + "account first.";
    public static final String PROVIDER_USER_ID = "109876543210";
    public static final String OTHER_PROVIDER_USER_ID = "otherAccountId";
    public static final String EMAIL_TOO_LONG = "Email must be at most 254 characters.";
    // @Email allows at most 64 characters before the @ and 63 per domain label, so the length comes from several labels.
    public static final String LONGEST_EMAIL = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
    public static final String OVERLONG_EMAIL = LONGEST_EMAIL + "d";
    // The update request's email is in mixed case, users store emails lowercased.
    public static final String UPDATED_EMAIL = "UpdatedEmail@shire.me";
    public static final String STORED_UPDATED_EMAIL = UPDATED_EMAIL.toLowerCase(Locale.ROOT);

    private static final Pattern TOKEN = Pattern.compile("token=([\\w-]+)");

    private UserTestData() {
    }

    public static String userNotFoundMessage(final long id) {
        return "User with id '" + id + "' not found.";
    }

    public static String userNameConflictMessage(final String userName) {
        return "User with username '" + userName + "' already exists.";
    }

    public static String emailConflictMessage(final String email) {
        return "User with email '" + email + "' already exists.";
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
                .setPassword(new BCryptPasswordEncoder().encode(TestUtils.USER_PASSWORD))
                .setRole(Role.USER)
                .setEnabled(true)
                .setCreated(Instant.now())
                .setUpdated(Instant.now())
                .setExams(new ArrayList<>());
    }

    public static User createTestUserPippin() {
        return createTestUser("Peregrin", "Tuk", "pippin", "pippin@shire.me");
    }

    public static User createMerry() {
        return createTestUser("Meriadok", "Brandybuck", "merry", "merry@shire.me");
    }

    public static User createUnverifiedPippin() {
        return createTestUserPippin().setEnabled(false);
    }

    public static User createGooglePippin() {
        return createTestUserPippin()
                .setPassword(null)
                .addIdentity(IdentityProvider.GOOGLE, PROVIDER_USER_ID);
    }

    /**
     * Profile of Pippin's account at the provider, with his verified email hosted by the provider.
     */
    public static ProviderProfile createPippinProfile(final IdentityProvider provider) {
        final var pippin = createTestUserPippin();
        return new ProviderProfile(provider, PROVIDER_USER_ID, pippin.getEmail(), EmailStatus.AUTHORITATIVE,
                pippin.getUserName(), pippin.getFirstName(), pippin.getLastName());
    }

    public static User createPippinChangingEmail() {
        return createTestUserPippin()
                .setPendingEmail(createValidUserUpdateRequest().email())
                .setVerificationTokenHash("pendingTokenHash")
                .setCurrentEmailTokenHash("currentTokenHash")
                .setVerificationTokenExpires(Instant.now().plus(Duration.ofHours(1)));
    }

    public static User createGimli() {
        return createTestUser("Gimli", "Gloinson", TestUtils.ADMIN_NAME, "gimli@erebor.me")
                .setRole(Role.ADMIN)
                .setPassword(new BCryptPasswordEncoder().encode(TestUtils.ADMIN_PASSWORD));
    }

    public static UserDto createUserDto(final long id) {
        return createUserDto(id, "pippin");
    }

    public static UserDto createUserDto(final long id, final String userName) {
        return new UserDto(
                id,
                "Peregrin",
                "Tuk",
                userName,
                "pippin@shire.me",
                null,
                false,
                false,
                Role.USER,
                true,
                List.of());
    }

    public static UserDto createAdminUserDto(final long id) {
        return new UserDto(
                id,
                "Gimli",
                "Gloinson",
                TestUtils.ADMIN_NAME,
                "gimli@erebor.me",
                null,
                false,
                false,
                Role.ADMIN,
                true,
                List.of());
    }

    public static UserDto createUpdatedUserDto(final long id) {
        return new UserDto(
                id,
                "UpdatedFirstName",
                "UpdatedLastName",
                "UpdatedUserName",
                "pippin@shire.me",
                STORED_UPDATED_EMAIL,
                true,
                true,
                Role.USER,
                true,
                List.of()
        );
    }

    public static UserDto createUpdatedByAdminUserDto(final long id) {
        final var updated = createUpdatedUserDto(id);
        return new UserDto(
                id,
                updated.firstName(),
                updated.lastName(),
                updated.userName(),
                updated.email(),
                null,
                false,
                false,
                updated.role(),
                updated.hasPassword(),
                updated.providers()
        );
    }

    public static UserUpdateRequest createValidUserUpdateRequest() {
        return new UserUpdateRequest(
                "UpdatedFirstName",
                "UpdatedLastName",
                "UpdatedUserName",
                UPDATED_EMAIL);
    }

    public static UserUpdateRequest createUserUpdateRequestWithoutEmail() {
        final var request = createValidUserUpdateRequest();
        return new UserUpdateRequest(request.firstName(), request.lastName(), request.userName(), null);
    }

    public static UserUpdateRequest createInvalidUserUpdateRequest() {
        return new UserUpdateRequest("", "", "gimli", "gimli.shire.me");
    }

    public static ChangePasswordRequest createValidChangePasswordRequest() {
        return new ChangePasswordRequest(TestUtils.USER_PASSWORD, "racuchy");
    }

    public static ChangePasswordRequest createWrongCurrentPasswordRequest() {
        return new ChangePasswordRequest(WRONG_PASSWORD, "drugieSniadanie");
    }

    public static ChangePasswordRequest createSameAsCurrentPasswordRequest() {
        return new ChangePasswordRequest(TestUtils.USER_PASSWORD, TestUtils.USER_PASSWORD);
    }

    public static DeleteAccountRequest createValidDeleteAccountRequest() {
        return new DeleteAccountRequest(TestUtils.USER_PASSWORD);
    }

    public static DeleteAccountRequest createWrongPasswordDeleteAccountRequest() {
        return new DeleteAccountRequest(WRONG_PASSWORD);
    }

    public static ResetPasswordRequest createValidResetPasswordRequest(final String token) {
        return new ResetPasswordRequest(token, "racuchy");
    }

    public static String extractToken(final SimpleMailMessage message) {
        return TOKEN.matcher(message.getText())
                .results()
                .map(r -> r.group(1))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No token in: " + message.getText()));
    }

    public static RegisterDto createValidRegisterDto() {
        return new RegisterDto(
                "Peregrin",
                "Tuk",
                "pippin",
                "pippin@shire.me",
                TestUtils.USER_PASSWORD);
    }

}
