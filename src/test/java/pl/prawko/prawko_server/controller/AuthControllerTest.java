package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.ForgotPasswordRequest;
import pl.prawko.prawko_server.dto.LoginDto;
import pl.prawko.prawko_server.dto.ResendVerificationRequest;
import pl.prawko.prawko_server.dto.ResetPasswordRequest;
import pl.prawko.prawko_server.dto.VerifyEmailRequest;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.implementation.EmailVerificationService;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@IntegrationTest
public class AuthControllerTest {

    private static final String USERNAME_SIZE_MSG = "Username or email must not be blank and between 3 and 63 characters.";
    private static final String PASSWORD_SIZE_MSG = "Password must not be blank and between 7 and 63 characters.";
    private static final long MAIL_SETTLE_MS = 500;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Value("${password-reset.cooldown}")
    private Duration cooldown;

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

    @Test
    void login_returnForbidden_whenEmailIsNotVerified() {
        restClient.post()
                .body(new LoginDto(TestUtils.UNVERIFIED_NAME, TestUtils.UNVERIFIED_PASSWORD))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(UserTestData.EMAIL_NOT_VERIFIED);
    }

    @Test
    void login_returnUnauthorized_whenEmailIsNotVerifiedAndPasswordIsWrong() {
        restClient.post()
                .body(new LoginDto(TestUtils.UNVERIFIED_NAME, "wrongPassword"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(TestUtils.INVALID_CREDENTIALS);
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
    @ExtendWith(OutputCaptureExtension.class)
    void login_notLogAttemptedLogin_whenCredentialsAreInvalid(final CapturedOutput output) {
        final var login = UserTestData.createMerry().getEmail();

        restClient.post()
                .body(new LoginDto(login, "wrongPassword"))
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(output).contains("Authentication failed.")
                .doesNotContain(login);
    }

    @Test
    void login_returnUnauthorized_whenCredentialsAreInvalid() {
        final var request = new LoginDto("nonExistentUser", "wrongPassword");
        final var expectedMessage = TestUtils.INVALID_CREDENTIALS;

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
    void forgotPassword_returnAcceptedWithoutWaitingForEmail_whenEmailExists() {
        final var tester = userRepository.save(UserTestData.createTestUserPippin());
        final var release = new CountDownLatch(1);
        final var sent = new AtomicBoolean();
        doAnswer(invocation -> {
            release.await(TestUtils.ASYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            sent.set(true);
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));

        forgotPassword(tester.getEmail());

        assertThat(sent).isFalse();
        release.countDown();
        verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS)).send(any(SimpleMailMessage.class));
    }

    @Test
    void forgotPassword_returnAcceptedWithoutEmail_whenEmailDoesNotExist() {
        forgotPassword(UserTestData.createTestUserPippin().getEmail());

        verify(mailSender, after(MAIL_SETTLE_MS).never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void forgotPassword_returnAcceptedWithoutEmail_whenRequestedWithinCooldown() {
        final var tester = userRepository.save(UserTestData.createTestUserPippin());
        final var token = requestResetToken(tester.getEmail());

        forgotPassword(tester.getEmail());

        verify(mailSender, after(MAIL_SETTLE_MS)).send(any(SimpleMailMessage.class));
        resetPassword(UserTestData.createValidResetPasswordRequest(token))
                .expectStatus().isNoContent();
    }

    @Test
    void forgotPassword_emailNewTokenAndInvalidateOldOne_whenCooldownHasPassed() {
        final var tester = userRepository.save(UserTestData.createTestUserPippin());
        final var oldToken = requestResetToken(tester.getEmail());
        final var pending = userRepository.findById(tester.getId()).orElseThrow();
        userRepository.save(pending.setPasswordResetTokenExpires(pending.getPasswordResetTokenExpires().minus(cooldown)));

        forgotPassword(tester.getEmail());

        final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS).times(2)).send(captor.capture());
        final var newToken = UserTestData.extractToken(captor.getValue());
        assertThat(newToken).isNotEqualTo(oldToken);
        resetPassword(UserTestData.createValidResetPasswordRequest(oldToken))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(UserTestData.INVALID_RESET_TOKEN);
        resetPassword(UserTestData.createValidResetPasswordRequest(newToken))
                .expectStatus().isNoContent();
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

    @Test
    void forgotPassword_returnServiceUnavailable_whenTaskQueueIsFull() {
        final var release = TestUtils.blockTaskExecutor(taskExecutor);
        try {
            restClient.post()
                    .uri(ApiConstants.FORGOT_PASSWORD)
                    .body(new ForgotPasswordRequest(UserTestData.createTestUserPippin().getEmail()))
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.SERVER_BUSY);
        } finally {
            release.countDown();
        }
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

    @Test
    void verifyEmail_returnNoContentAndEnableUser_whenTokenIsValid() {
        final var tester = userRepository.save(UserTestData.createUnverifiedPippin());

        verifyEmail(requestVerificationToken(tester.getEmail()))
                .expectStatus().isNoContent();

        final var verified = userRepository.findById(tester.getId()).orElseThrow();
        assertThat(verified.isEnabled()).isTrue();
        assertThat(verified.getVerificationTokenHash()).isNull();
        assertThat(verified.getVerificationTokenExpires()).isNull();
    }

    @Test
    void verifyEmail_returnNoContentAndChangeEmail_whenEmailChangeIsPending() {
        final var tester = UserTestData.createPippinChangingEmail();
        final var newEmail = tester.getPendingEmail();
        final var token = emailVerificationService.issueToken(tester);
        userRepository.save(tester);

        verifyEmail(token)
                .expectStatus().isNoContent();

        final var changed = userRepository.findById(tester.getId()).orElseThrow();
        assertThat(changed.getEmail()).isEqualTo(newEmail);
        assertThat(changed.getPendingEmail()).isNull();
        assertThat(changed.getVerificationTokenHash()).isNull();
    }

    @Test
    void verifyEmail_returnConflictAndKeepEmail_whenPendingEmailWasTakenMeanwhile() {
        final var tester = UserTestData.createPippinChangingEmail();
        final var email = tester.getEmail();
        final var newEmail = tester.getPendingEmail();
        final var token = emailVerificationService.issueToken(tester);
        userRepository.save(tester);
        userRepository.save(UserTestData.createMerry().setEmail(newEmail));

        verifyEmail(token)
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.detail").isEqualTo(UserTestData.USER_ALREADY_EXISTS)
                .jsonPath("$.details.email").isEqualTo("User with email '" + newEmail + "' already exists.");

        final var unchanged = userRepository.findById(tester.getId()).orElseThrow();
        assertThat(unchanged.getEmail()).isEqualTo(email);
        assertThat(unchanged.getPendingEmail()).isEqualTo(newEmail);
    }

    @Test
    void verifyEmail_returnBadRequest_whenTokenIsAlreadyUsed() {
        final var tester = userRepository.save(UserTestData.createUnverifiedPippin());
        final var token = requestVerificationToken(tester.getEmail());
        verifyEmail(token)
                .expectStatus().isNoContent();

        verifyEmail(token)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(UserTestData.INVALID_VERIFICATION_TOKEN);
    }

    @Test
    void verifyEmail_returnBadRequest_whenTokenIsUnknown() {
        verifyEmail("unknownToken")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(UserTestData.INVALID_VERIFICATION_TOKEN);
    }

    @Test
    void verifyEmail_returnBadRequest_whenTokenIsBlank() {
        verifyEmail(" ")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(TestUtils.VALIDATION_FAILED)
                .jsonPath("$.details.token").isEqualTo("Token is required.");
    }

    @Test
    void resendVerification_returnAcceptedAndEmailLink_whenUserIsUnverified() {
        final var tester = userRepository.save(UserTestData.createUnverifiedPippin());

        resendVerification(tester.getEmail().toUpperCase());

        assertThat(captureSentMessage().getTo()).containsExactly(tester.getEmail());
        assertThat(userRepository.findById(tester.getId()))
                .get().extracting(User::getVerificationTokenHash).isNotNull();
    }

    @Test
    void resendVerification_returnAcceptedWithoutEmail_whenUserIsVerified() {
        final var tester = userRepository.save(UserTestData.createTestUserPippin());

        resendVerification(tester.getEmail());

        verify(mailSender, after(MAIL_SETTLE_MS).never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void resendVerification_returnAcceptedWithoutEmail_whenEmailDoesNotExist() {
        resendVerification(UserTestData.createTestUserPippin().getEmail());

        verify(mailSender, after(MAIL_SETTLE_MS).never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void resendVerification_returnBadRequest_whenEmailIsInvalid() {
        restClient.post()
                .uri(ApiConstants.RESEND_VERIFICATION)
                .body(new ResendVerificationRequest("pippin.shire.me"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(TestUtils.VALIDATION_FAILED)
                .jsonPath("$.details.email").isEqualTo("Email format is not valid.");
        verifyNoInteractions(mailSender);
    }

    private String requestVerificationToken(final String email) {
        resendVerification(email);
        return UserTestData.extractToken(captureSentMessage());
    }

    private void resendVerification(final String email) {
        restClient.post()
                .uri(ApiConstants.RESEND_VERIFICATION)
                .body(new ResendVerificationRequest(email))
                .exchange()
                .expectStatus().isAccepted();
    }

    private RestTestClient.ResponseSpec verifyEmail(final String token) {
        return restClient.post()
                .uri(ApiConstants.VERIFY_EMAIL)
                .body(new VerifyEmailRequest(token))
                .exchange();
    }

    private String requestResetToken(final String email) {
        forgotPassword(email);
        return UserTestData.extractToken(captureSentMessage());
    }

    private RestTestClient.ResponseSpec resetPassword(final ResetPasswordRequest request) {
        return restClient.post()
                .uri(ApiConstants.RESET_PASSWORD)
                .body(request)
                .exchange();
    }

    private SimpleMailMessage captureSentMessage() {
        final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS)).send(captor.capture());
        return captor.getValue();
    }

    private void forgotPassword(final String email) {
        restClient.post()
                .uri(ApiConstants.FORGOT_PASSWORD)
                .body(new ForgotPasswordRequest(email))
                .exchange()
                .expectStatus().isAccepted();
    }

}
