package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.ForgotPasswordRequest;
import pl.prawko.prawko_server.dto.LoginDto;
import pl.prawko.prawko_server.dto.ResetPasswordRequest;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@IntegrationTest
public class AuthControllerTest {

    private static final String USERNAME_SIZE_MSG = "Username or email must not be blank and between 3 and 63 characters.";
    private static final String PASSWORD_SIZE_MSG = "Password must not be blank and between 7 and 63 characters.";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MailSender mailSender;

    @LocalServerPort
    private int port;

    private RestTestClient restClient;

    private static Stream<Arguments> invalidLoginRequests() {
        return Stream.of(
                Arguments.of("both too short", new LoginDto("a".repeat(2), "b".repeat(6)), USERNAME_SIZE_MSG, PASSWORD_SIZE_MSG),
                Arguments.of("both too long", new LoginDto("a".repeat(64), "b".repeat(64)), USERNAME_SIZE_MSG, PASSWORD_SIZE_MSG),
                Arguments.of("both null", new LoginDto(null, null), TestUtils.USERNAME_REQUIRED, TestUtils.PASSWORD_REQUIRED),
                Arguments.of("username blank", new LoginDto("", "lembasy"), USERNAME_SIZE_MSG, null),
                Arguments.of("username null", new LoginDto(null, "password"), TestUtils.USERNAME_REQUIRED, null),
                Arguments.of("password blank", new LoginDto("pippin", "  "), null, PASSWORD_SIZE_MSG),
                Arguments.of("password null", new LoginDto("pippin", null), null, TestUtils.PASSWORD_REQUIRED)
        );
    }

    @BeforeEach
    void setUp() {
        restClient = TestUtils.createRestTestClient(port, ApiConstants.AUTH_BASE_URL);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void login_returnOk_whenCredentialsAreValid() {
        final var request = new LoginDto(TestUtils.USER_NAME, TestUtils.USER_PASSWORD);
        final var expectedMessage = "User signed-in successfully.";

        restClient.post()
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(expectedMessage);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidLoginRequests")
    void login_returnBadRequest_whenValidationFails(
            final String name,
            final LoginDto request,
            final String expectedUserNameError,
            final String expectedPasswordError) {

        final var response = restClient.post()
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(TestUtils.VALIDATION_FAILED);

        if (expectedUserNameError != null) {
            response.jsonPath("$.details.userName").isEqualTo(expectedUserNameError);
        }
        if (expectedPasswordError != null) {
            response.jsonPath("$.details.password").isEqualTo(expectedPasswordError);
        }
    }

    @Test
    void login_passValidation_whenEmailIsLongerThanUsernameLimit() {
        final var request = new LoginDto("meriadoc.brandybuck@buckland.shire.me", "lembasy");

        restClient.post()
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void login_returnUnauthorized_whenCredentialsAreInvalid() {
        final var request = new LoginDto("nonExistentUser", "wrongPassword");
        final var expectedMessage = "Bad credentials";

        restClient.post()
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(expectedMessage);
    }

    @Test
    void login_returnBadRequest_whenBodyIsMissing() {
        final var expectedMessage = TestUtils.BODY_MISSING;

        restClient.post()
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(expectedMessage);
    }

    @Test
    void forgotPassword_returnAcceptedAndEmailResetLink_whenEmailExists() {
        final var tester = userRepository.save(UserTestData.createTestUserPippin());

        forgotPassword(tester.getEmail().toUpperCase());

        assertThat(captureSentMessage().getTo()).containsExactly(tester.getEmail());
        assertThat(userRepository.findById(tester.getId()))
                .get().extracting(User::getPasswordResetTokenHash).isNotNull();
    }

    @Test
    void forgotPassword_returnAcceptedWithoutEmail_whenEmailDoesNotExist() {
        forgotPassword(UserTestData.createTestUserPippin().getEmail());

        verifyNoInteractions(mailSender);
    }

    @Test
    void forgotPassword_returnBadRequest_whenEmailIsInvalid() {
        restClient.post()
                .uri(ApiConstants.FORGOT_PASSWORD)
                .body(new ForgotPasswordRequest("pippin.shire.me"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(TestUtils.VALIDATION_FAILED)
                .jsonPath("$.details.email").isEqualTo("Email format is not valid.");
        verifyNoInteractions(mailSender);
    }

    private void forgotPassword(final String email) {
        restClient.post()
                .uri(ApiConstants.FORGOT_PASSWORD)
                .body(new ForgotPasswordRequest(email))
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    void resetPassword_returnNoContentAndChangePassword_whenTokenIsValid() {
        final var tester = userRepository.save(UserTestData.createTestUserPippin());
        final var request = UserTestData.createValidResetPasswordRequest(requestResetToken(tester.getEmail()));

        resetPassword(request)
                .expectStatus().isNoContent();

        final var updated = userRepository.findById(tester.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(request.newPassword(), updated.getPassword())).isTrue();
        assertThat(updated.getPasswordResetTokenHash()).isNull();
        assertThat(updated.getPasswordResetTokenExpires()).isNull();
    }

    @Test
    void resetPassword_returnBadRequest_whenTokenIsAlreadyUsed() {
        final var tester = userRepository.save(UserTestData.createTestUserPippin());
        final var request = UserTestData.createValidResetPasswordRequest(requestResetToken(tester.getEmail()));
        resetPassword(request)
                .expectStatus().isNoContent();

        resetPassword(request)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(UserTestData.INVALID_RESET_TOKEN);
    }

    @Test
    void resetPassword_returnBadRequest_whenTokenIsUnknown() {
        resetPassword(UserTestData.createValidResetPasswordRequest("unknownToken"))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(UserTestData.INVALID_RESET_TOKEN);
    }

    @Test
    void resetPassword_returnBadRequest_whenValidationFails() {
        resetPassword(new ResetPasswordRequest(" ", "short"))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(TestUtils.VALIDATION_FAILED)
                .jsonPath("$.details.token").isEqualTo("Token is required.")
                .jsonPath("$.details.newPassword").isEqualTo("Password must be at least 7 characters.");
    }

    private String requestResetToken(final String email) {
        forgotPassword(email);
        return UserTestData.extractResetToken(captureSentMessage());
    }

    private RestTestClient.ResponseSpec resetPassword(final ResetPasswordRequest request) {
        return restClient.post()
                .uri(ApiConstants.RESET_PASSWORD)
                .body(request)
                .exchange();
    }

    private SimpleMailMessage captureSentMessage() {
        final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

}
