package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.PageResponse;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.ChangePasswordRequest;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.dto.UserUpdateRequest;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.test_data.UserTestData;
import pl.prawko.prawko_server.util.SecureTokens;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@IntegrationTest
public class UserControllerTest {

    private static final String USERNAME_CONTAINS_AT_MSG = "Username must not contain '@'.";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @LocalServerPort
    private int port;

    private RestTestClient restClient;

    private final RegisterDto registerDto = UserTestData.createValidRegisterDto();

    @BeforeEach
    void setUp() {
        restClient = TestUtils.createRestTestClient(port, ApiConstants.USERS_BASE_URL);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Nested
    class RegisterUser {

        @Test
        void success_whenDtoIsValid() {
            restClient.post()
                    .body(registerDto)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().value("Location", location -> {
                        final var createdUser = userRepository.findAll().getFirst();
                        final var expectedLocation = ApiConstants.USERS_BASE_URL + "/" + createdUser.getId();
                        assertThat(location).endsWith(expectedLocation);
                    });
        }

        @Test
        void storeUnverifiedUserAndEmailVerificationLink_whenDtoIsValid() {
            final var id = registerUser();

            final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS)).send(captor.capture());
            final var user = userRepository.findById(id).orElseThrow();
            assertThat(captor.getValue().getTo()).containsExactly(registerDto.email());
            assertThat(UserTestData.extractToken(captor.getValue())).isNotBlank();
            assertThat(user.isEnabled()).isFalse();
            assertThat(user.getVerificationTokenHash()).isNotNull();
        }

        @Test
        void returnServiceUnavailableAndNotStoreUser_whenTaskQueueIsFull() {
            final var release = TestUtils.blockTaskExecutor(taskExecutor);
            try {
                restClient.post()
                        .body(registerDto)
                        .exchange()
                        .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                        .expectBody()
                        .jsonPath("$.detail").isEqualTo(TestUtils.SERVER_BUSY);
            } finally {
                release.countDown();
            }
            assertThat(userRepository.count()).isZero();
        }

        @Test
        void returnBadRequest_whenDtoIsInvalid() {
            final var invalidDto = new RegisterDto(
                    "Supercalifragilisticexpialidocious",
                    null,
                    "OK",
                    "notValidMail@mail@mail",
                    "lembas");
            final var expectedMap = Map.ofEntries(
                    Map.entry("message", TestUtils.VALIDATION_FAILED),
                    Map.entry("details", Map.ofEntries(
                            Map.entry("firstName", "First name must be at most 31 characters."),
                            Map.entry("lastName", "Last name is required."),
                            Map.entry("userName", "Username must be at least 3 characters."),
                            Map.entry("email", "Email format is not valid."),
                            Map.entry("password", "Password must be at least 7 characters."))
                    ));

            restClient.post()
                    .body(invalidDto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMap.get("message"))
                    .jsonPath("$.details").isEqualTo(expectedMap.get("details"));
        }

        @Test
        void returnBadRequest_whenUserNameContainsAt() {
            final var invalidDto = new RegisterDto("Peregrin", "Tuk", "pippin@shire", "pippin@shire.me", "lembasy");

            restClient.post()
                    .body(invalidDto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.details.userName").isEqualTo(USERNAME_CONTAINS_AT_MSG);
        }

        @Test
        void returnBadRequest_whenBodyIsMissing() {
            final var expectedMessage = TestUtils.BODY_MISSING;

            restClient.post()
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMessage);
        }

        @Test
        void returnBadRequest_whenAllFieldsAreNull() {
            registerUser();
            final var invalidDto = new RegisterDto(null, null, null, null, null);
            final var expected = Map.ofEntries(
                    Map.entry("message", TestUtils.VALIDATION_FAILED),
                    Map.entry("details", Map.ofEntries(
                            Map.entry("firstName", "First name is required."),
                            Map.entry("lastName", "Last name is required."),
                            Map.entry("userName", TestUtils.USERNAME_REQUIRED),
                            Map.entry("email", "Email is required."),
                            Map.entry("password", TestUtils.PASSWORD_REQUIRED))));

            restClient.post()
                    .body(invalidDto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expected.get("message"))
                    .jsonPath("$.details").isEqualTo(expected.get("details"));
        }

        @Test
        void returnConflict_whenEmailDiffersOnlyInCase() {
            registerUser();
            final var dto = new RegisterDto("Peregrin", "Tuk", "peregrin", "Pippin@Shire.ME", "lembasy");

            restClient.post()
                    .body(dto)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.details.email").isEqualTo("User with email 'Pippin@Shire.ME' already exists.")
                    .jsonPath("$.details.userName").doesNotExist();
        }

        @Test
        void returnConflict_whenUserAlreadyExists() {
            registerUser();
            final var expected = Map.ofEntries(
                    Map.entry("message", UserTestData.USER_ALREADY_EXISTS),
                    Map.entry("details", UserTestData.PIPPIN_CONFLICT_DETAILS));

            restClient.post()
                    .body(registerDto)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expected.get("message"))
                    .jsonPath("$.details").isEqualTo(expected.get("details"));
        }

    }

    @Nested
    class GetUserById {

        @Test
        void returnUserDto_whenFound() {
            final var id = registerUser();
            final var expectedUserDto = UserTestData.createUserDto(id);

            restClient.get()
                    .uri(ApiConstants.BY_ID, id)
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class).isEqualTo(expectedUserDto);
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.get()
                    .uri(ApiConstants.BY_ID, 1L)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }


        @Test
        void returnBadRequest_whenIdIsZero() {
            final var expectedMessage = TestUtils.ID_NOT_POSITIVE;

            restClient.get()
                    .uri(ApiConstants.BY_ID, 0L)
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMessage);
        }

        @Test
        void returnNotFound_whenUserDoesNotExist() {
            final var nonExistentId = 666L;
            final var expectedMessage = UserTestData.userNotFoundMessage(nonExistentId);

            restClient.get()
                    .uri(ApiConstants.BY_ID, nonExistentId)
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMessage);
        }

        @Test
        void returnBadRequest_whenIdIsNegative() {
            final var negativeId = -1L;
            final var expectedMessage = TestUtils.ID_NOT_POSITIVE;

            restClient.get()
                    .uri(ApiConstants.BY_ID, negativeId)
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMessage);
        }

    }

    @Nested
    class GetCurrentUser {

        @Test
        void returnUserDto_whenAuthenticatedAsUser() {
            final var id = registerUser();
            final var expectedUserDto = UserTestData.createUserDto(id);

            restClient.get()
                    .uri(ApiConstants.ME)
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class).isEqualTo(expectedUserDto);
        }

        @Test
        void returnAdminRole_whenAuthenticatedAsAdmin() {
            final var id = userRepository.save(UserTestData.createGimli()).getId();
            final var expectedUserDto = UserTestData.createAdminUserDto(id);

            restClient.get()
                    .uri(ApiConstants.ME)
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class).isEqualTo(expectedUserDto);
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.get()
                    .uri(ApiConstants.ME)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

    }

    @Nested
    class GetAllUsers {

        @Test
        void returnPage_whenUsersExist() {
            final var id = registerUser();
            final var expectedUserDto = UserTestData.createUserDto(id);
            final var expected = List.of(expectedUserDto);

            final var result = restClient.get()
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<PageResponse<UserDto>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertThat(result.content()).isEqualTo(expected);
            assertThat(result.page().number()).isZero();
            assertThat(result.page().size()).isEqualTo(20);
            assertThat(result.page().totalElements()).isEqualTo(1);
        }

        @Test
        void returnRequestedPage_whenPageSizeAndSortAreGiven() {
            userRepository.save(UserTestData.createTestUserPippin());
            final var merry = userRepository.save(UserTestData.createMerry());
            userRepository.save(UserTestData.createTestUser("Samwise", "Gamgee", "sam", "sam@shire.me"));

            restClient.get()
                    .uri(uri -> uri
                            .queryParam("page", 1)
                            .queryParam("size", 2)
                            .queryParam("sort", "userName,desc")
                            .build())
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content.length()").isEqualTo(1)
                    .jsonPath("$.content[0].id").isEqualTo(merry.getId())
                    .jsonPath("$.page.totalElements").isEqualTo(3)
                    .jsonPath("$.page.totalPages").isEqualTo(2);
        }

        @Test
        void capPageSize_whenRequestedSizeExceedsMaximum() {
            restClient.get()
                    .uri(uri -> uri.queryParam("size", 1000).build())
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.page.size").isEqualTo(100);
        }

        @Test
        void returnEmptyPage_whenNoUsersExist() {
            restClient.get()
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content").isEmpty()
                    .jsonPath("$.page.totalElements").isEqualTo(0);
        }

        @Test
        void returnBadRequest_whenSortPropertyIsInvalid() {
            restClient.get()
                    .uri(uri -> uri.queryParam("sort", "nonExisting").build())
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.INVALID_SORT);
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.get()
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

    }

    @Nested
    class UpdateUser {

        @Test
        void success_whenDtoIsValid() {
            final var id = userRepository.save(UserTestData.createTestUserPippin()).getId();
            final var validDto = UserTestData.createValidUserUpdateRequest();
            final var expected = UserTestData.createUpdatedUserDto(id);

            restClient.patch()
                    .uri(ApiConstants.BY_ID, id)
                    .headers(TestUtils::authAdmin)
                    .body(validDto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class).isEqualTo(expected);
        }

        @Test
        void success_whenUserUpdatesThemselves() {
            final var id = userRepository.save(UserTestData.createTestUserPippin()).getId();
            final var validDto = UserTestData.createValidUserUpdateRequest();
            final var expected = UserTestData.createUpdatedUserDto(id);

            restClient.patch()
                    .uri(ApiConstants.BY_ID, id)
                    .headers(TestUtils::authUser)
                    .body(validDto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class).isEqualTo(expected);
        }

        @Test
        void keepEmailAndSendLinksToNewAndCurrentEmail_whenOwnEmailChanged() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var newEmail = UserTestData.createValidUserUpdateRequest().email();

            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(TestUtils::authUser)
                    .body(new UserUpdateRequest(null, null, null, newEmail))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.email").isEqualTo(tester.getEmail())
                    .jsonPath("$.pendingEmail").isEqualTo(newEmail);

            final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS).times(2)).send(captor.capture());
            final var link = captor.getAllValues().getFirst();
            final var confirmation = captor.getAllValues().getLast();
            assertThat(link.getTo()).containsExactly(newEmail);
            assertThat(confirmation.getTo()).containsExactly(tester.getEmail());
            final var updated = userRepository.findById(tester.getId()).orElseThrow();
            assertThat(updated.getEmail()).isEqualTo(tester.getEmail());
            assertThat(updated.getPendingEmail()).isEqualTo(newEmail);
            assertThat(updated.getVerificationTokenHash())
                    .isEqualTo(SecureTokens.hash(UserTestData.extractToken(link)));
            assertThat(updated.getCurrentEmailTokenHash())
                    .isEqualTo(SecureTokens.hash(UserTestData.extractToken(confirmation)));
        }

        @Test
        void requireOnlyNewEmailLinkAndNotifyCurrentEmail_whenAdminChangesEmail() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var newEmail = UserTestData.createValidUserUpdateRequest().email();

            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(TestUtils::authAdmin)
                    .body(new UserUpdateRequest(null, null, null, newEmail))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.pendingEmail").isEqualTo(newEmail);

            final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS).times(2)).send(captor.capture());
            final var notice = captor.getAllValues().getLast();
            assertThat(notice.getTo()).containsExactly(tester.getEmail());
            assertThat(notice.getText()).doesNotContain("token=");
            final var updated = userRepository.findById(tester.getId()).orElseThrow();
            assertThat(updated.getVerificationTokenHash()).isNotNull();
            assertThat(updated.getCurrentEmailTokenHash()).isNull();
        }

        @Test
        void returnTooManyRequestsAndKeepPendingEmail_whenEmailChangedAgainWithinCooldown() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var firstEmail = UserTestData.createValidUserUpdateRequest().email();
            final var secondEmail = UserTestData.createMerry().getEmail();
            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(TestUtils::authUser)
                    .body(new UserUpdateRequest(null, null, null, firstEmail))
                    .exchange()
                    .expectStatus().isOk();
            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS).times(2)).send(any(SimpleMailMessage.class));

            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(TestUtils::authUser)
                    .body(new UserUpdateRequest(null, null, null, secondEmail))
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.VERIFICATION_COOLDOWN);

            assertThat(userRepository.findById(tester.getId()).orElseThrow().getPendingEmail()).isEqualTo(firstEmail);
            verifyNoMoreInteractions(mailSender);
        }

        @Test
        void cancelEmailChangeWithinCooldown_whenCurrentEmailSent() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var newEmail = UserTestData.createValidUserUpdateRequest().email();
            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(TestUtils::authUser)
                    .body(new UserUpdateRequest(null, null, null, newEmail))
                    .exchange()
                    .expectStatus().isOk();
            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS).times(2)).send(any(SimpleMailMessage.class));

            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(TestUtils::authUser)
                    .body(new UserUpdateRequest(null, null, null, tester.getEmail()))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.email").isEqualTo(tester.getEmail())
                    .jsonPath("$.pendingEmail").isEmpty();

            final var cancelled = userRepository.findById(tester.getId()).orElseThrow();
            assertThat(cancelled.getPendingEmail()).isNull();
            assertThat(cancelled.getVerificationTokenHash()).isNull();
            verifyNoMoreInteractions(mailSender);
        }

        @Test
        void returnServiceUnavailableAndNotChangeUser_whenEmailChangedAndTaskQueueIsFull() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var release = TestUtils.blockTaskExecutor(taskExecutor);
            try {
                restClient.patch()
                        .uri(ApiConstants.BY_ID, tester.getId())
                        .headers(TestUtils::authUser)
                        .body(UserTestData.createValidUserUpdateRequest())
                        .exchange()
                        .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                        .expectBody()
                        .jsonPath("$.detail").isEqualTo(TestUtils.SERVER_BUSY);
            } finally {
                release.countDown();
            }
            final var unchanged = userRepository.findById(tester.getId()).orElseThrow();
            assertThat(unchanged.getUserName()).isEqualTo(tester.getUserName());
            assertThat(unchanged.getPendingEmail()).isNull();
        }

        @Test
        void returnForbidden_whenUserUpdatesAnotherUser() {
            registerUser();
            final var other = userRepository.save(UserTestData.createMerry());

            restClient.patch()
                    .uri(ApiConstants.BY_ID, other.getId())
                    .headers(TestUtils::authUser)
                    .body(UserTestData.createValidUserUpdateRequest())
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.ACCESS_DENIED);

            assertThat(userRepository.findById(other.getId()).orElseThrow().getUserName()).isEqualTo("merry");
        }

        @Test
        void returnBadRequest_whenDtoIsInvalid() {
            final var invalidUpdateRequest = UserTestData.createInvalidUserUpdateRequest();
            final var expected = Map.ofEntries(
                    Map.entry("message", TestUtils.VALIDATION_FAILED),
                    Map.entry("details", Map.ofEntries(
                            Map.entry("firstName", "First name must be at least 3 characters."),
                            Map.entry("lastName", "Last name must be at least 3 characters."),
                            Map.entry("email", "Email format is not valid."))));

            restClient.patch()
                    .uri(ApiConstants.BY_ID, 1L)
                    .headers(TestUtils::authUser)
                    .body(invalidUpdateRequest)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expected.get("message"))
                    .jsonPath("$.details").isEqualTo(expected.get("details"));
        }

        @Test
        void returnBadRequest_whenUserNameContainsAt() {
            final var invalidUpdateRequest = new UserUpdateRequest(null, null, "pippin@shire", null);

            restClient.patch()
                    .uri(ApiConstants.BY_ID, 1L)
                    .headers(TestUtils::authUser)
                    .body(invalidUpdateRequest)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.details.userName").isEqualTo(USERNAME_CONTAINS_AT_MSG);
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, 0L})
        void returnBadRequest_whenIdIsNotPositive(final long invalidId) {
            final var validDto = UserTestData.createValidUserUpdateRequest();
            final var expectedMessage = TestUtils.ID_NOT_POSITIVE;

            restClient.patch()
                    .uri(ApiConstants.BY_ID, invalidId)
                    .headers(TestUtils::authAdmin)
                    .body(validDto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMessage);
        }

        @Test
        void returnNotFound_whenUserDoesNotExist() {
            final var nonExistentId = 666L;
            final var validDto = UserTestData.createValidUserUpdateRequest();
            final var expectedMessage = UserTestData.userNotFoundMessage(nonExistentId);

            restClient.patch()
                    .uri(ApiConstants.BY_ID, nonExistentId)
                    .headers(TestUtils::authAdmin)
                    .body(validDto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMessage);
        }

        @Test
        void returnBadRequest_whenBodyIsMissing() {
            final var expectedMessage = TestUtils.BODY_MISSING;

            restClient.patch()
                    .uri(ApiConstants.BY_ID, 1L)
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMessage);
        }


        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.patch()
                    .uri(ApiConstants.BY_ID, 1L)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

    }

    @Nested
    class ChangePassword {

        @Test
        void returnNoContent_andChangePassword_whenRequestIsValid() {
            final var id = registerUser();
            final var request = UserTestData.createValidChangePasswordRequest();

            restClient.patch()
                    .uri(ApiConstants.PASSWORD)
                    .headers(TestUtils::authUser)
                    .body(request)
                    .exchange()
                    .expectStatus().isNoContent();

            final var storedPassword = userRepository.findById(id).orElseThrow().getPassword();
            assertThat(passwordEncoder.matches(request.newPassword(), storedPassword)).isTrue();
        }

        @Test
        void cancelEmailChange_whenEmailChangeIsPending() {
            final var tester = userRepository.save(UserTestData.createPippinChangingEmail());

            restClient.patch()
                    .uri(ApiConstants.PASSWORD)
                    .headers(TestUtils::authUser)
                    .body(UserTestData.createValidChangePasswordRequest())
                    .exchange()
                    .expectStatus().isNoContent();

            final var updated = userRepository.findById(tester.getId()).orElseThrow();
            assertThat(updated.getEmail()).isEqualTo(tester.getEmail());
            assertThat(updated.getPendingEmail()).isNull();
            assertThat(updated.getVerificationTokenHash()).isNull();
        }

        @Test
        void changeOnlyOwnPassword_whenOtherUsersExist() {
            registerUser();
            final var other = userRepository.save(UserTestData.createMerry());
            final var request = UserTestData.createValidChangePasswordRequest();

            restClient.patch()
                    .uri(ApiConstants.PASSWORD)
                    .headers(TestUtils::authUser)
                    .body(request)
                    .exchange()
                    .expectStatus().isNoContent();

            final var otherPassword = userRepository.findById(other.getId()).orElseThrow().getPassword();
            assertThat(passwordEncoder.matches(request.currentPassword(), otherPassword)).isTrue();
        }

        @Test
        void returnBadRequest_whenCurrentPasswordIsIncorrect() {
            final var id = registerUser();
            final var request = UserTestData.createWrongCurrentPasswordRequest();

            restClient.patch()
                    .uri(ApiConstants.PASSWORD)
                    .headers(TestUtils::authUser)
                    .body(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo("Current password is incorrect.");
        }

        @Test
        void returnBadRequest_whenNewPasswordIsSameAsCurrent() {
            final var id = registerUser();
            final var request = UserTestData.createSameAsCurrentPasswordRequest();

            restClient.patch()
                    .uri(ApiConstants.PASSWORD)
                    .headers(TestUtils::authUser)
                    .body(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo("New password must be different from the current one.");
        }

        @Test
        void returnBadRequest_whenDtoIsInvalid() {
            final var invalidRequest = new ChangePasswordRequest(" ", "short");
            final var expected = Map.ofEntries(
                    Map.entry("message", TestUtils.VALIDATION_FAILED),
                    Map.entry("details", Map.ofEntries(
                            Map.entry("currentPassword", "Current password is required."),
                            Map.entry("newPassword", "Password must be at least 7 characters."))));

            restClient.patch()
                    .uri(ApiConstants.PASSWORD)
                    .headers(TestUtils::authUser)
                    .body(invalidRequest)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expected.get("message"))
                    .jsonPath("$.details").isEqualTo(expected.get("details"));
        }

        @Test
        void returnBadRequest_whenBodyIsMissing() {
            restClient.patch()
                    .uri(ApiConstants.PASSWORD)
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.BODY_MISSING);
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.patch()
                    .uri(ApiConstants.PASSWORD)
                    .body(UserTestData.createValidChangePasswordRequest())
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

    }

    @Nested
    class DeleteUser {

        @Test
        void returnNoContent_whenSuccess() {
            final var id = registerUser();
            restClient.delete()
                    .uri(ApiConstants.BY_ID, id)
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isNoContent();
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.delete()
                    .uri(ApiConstants.BY_ID, 1L)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        void returnNotFound_whenUserDoesNotExist() {
            final var nonExistentId = 666L;
            final var expectedMessage = UserTestData.userNotFoundMessage(nonExistentId);

            restClient.delete()
                    .uri(ApiConstants.BY_ID, nonExistentId)
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMessage);
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, 0L})
        void returnBadRequest_whenIdIsNotPositive(final long invalidId) {
            final var expectedMessage = TestUtils.ID_NOT_POSITIVE;

            restClient.delete()
                    .uri(ApiConstants.BY_ID, invalidId)
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMessage);
        }

    }

    private long registerUser() {
        restClient.post()
                .body(registerDto)
                .exchangeSuccessfully();
        return userRepository.findAll().getFirst().getId();
    }

}
