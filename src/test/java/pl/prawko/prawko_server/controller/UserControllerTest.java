package pl.prawko.prawko_server.controller;

import org.jspecify.annotations.Nullable;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.PageResponse;
import pl.prawko.prawko_server.config.TestAuth;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.request.ChangePasswordRequest;
import pl.prawko.prawko_server.dto.request.DeleteAccountRequest;
import pl.prawko.prawko_server.dto.request.ForgotPasswordRequest;
import pl.prawko.prawko_server.dto.request.GitHubSignInRequest;
import pl.prawko.prawko_server.dto.request.GoogleSignInRequest;
import pl.prawko.prawko_server.dto.request.LinkGitHubRequest;
import pl.prawko.prawko_server.dto.request.LinkGoogleRequest;
import pl.prawko.prawko_server.dto.request.RegisterRequest;
import pl.prawko.prawko_server.dto.request.UnlinkIdentityRequest;
import pl.prawko.prawko_server.dto.request.UserUpdateRequest;
import pl.prawko.prawko_server.dto.response.AuthTokensDto;
import pl.prawko.prawko_server.dto.response.UserDto;
import pl.prawko.prawko_server.dto.response.UserStatsDto;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.ExamRepository;
import pl.prawko.prawko_server.repository.QuestionRepository;
import pl.prawko.prawko_server.repository.RefreshTokenRepository;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.test_data.ExamTestData;
import pl.prawko.prawko_server.test_data.GitHubTestData;
import pl.prawko.prawko_server.test_data.GoogleTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;
import pl.prawko.prawko_server.test_data.RefreshTokenTestData;
import pl.prawko.prawko_server.test_data.UserTestData;
import pl.prawko.prawko_server.util.SecureTokens;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@IntegrationTest
public class UserControllerTest {

    private static final String USERNAME_CONTAINS_AT_MSG = "Username must not contain '@'.";
    private static final String LAST_NAME_TOO_SHORT_MSG = "Last name must be at least 3 characters.";
    private static final String TOO_LONG_NAME = "Supercalifragilisticexpialidocious";

    @Autowired
    private TestAuth auth;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @LocalServerPort
    private int port;

    @Autowired
    private MockRestServiceServer github;

    private RestTestClient restClient;

    private final RegisterRequest registerRequest = UserTestData.createValidRegisterRequest();

    @BeforeEach
    void setUp() {
        restClient = TestUtils.createRestTestClient(port, ApiConstants.USERS_BASE_URL);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        questionRepository.deleteAll();
    }

    @Nested
    class RegisterUser {

        @Test
        void success_whenDtoIsValid() {
            restClient.post()
                    .body(registerRequest)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().value("Location", location -> {
                        final var createdUser = userRepository.findAll().getFirst();
                        final var expectedLocation = ApiConstants.USERS_BASE_URL + "/" + createdUser.getId();
                        assertThat(location).endsWith(expectedLocation);
                    });

            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS)).send(any(SimpleMailMessage.class));
        }

        @Test
        void storeUnverifiedUserAndEmailVerificationLink_whenDtoIsValid() {
            final var id = registerUser();

            final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS)).send(captor.capture());
            final var user = userRepository.findById(id).orElseThrow();
            assertThat(captor.getValue().getTo()).containsExactly(registerRequest.email());
            assertThat(UserTestData.extractToken(captor.getValue())).isNotBlank();
            assertThat(user.isEnabled()).isFalse();
            assertThat(user.getVerificationTokenHash()).isNotNull();
        }

        @Test
        void returnServiceUnavailableAndNotStoreUser_whenTaskQueueIsFull() {
            final var release = TestUtils.blockTaskExecutor(taskExecutor);
            try {
                restClient.post()
                        .body(registerRequest)
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
            final var invalidDto = new RegisterRequest(
                    TOO_LONG_NAME,
                    "Tu",
                    "OK",
                    "notValidMail@mail@mail",
                    "lembas");
            final var expectedMap = Map.ofEntries(
                    Map.entry("message", TestUtils.VALIDATION_FAILED),
                    Map.entry("details", Map.ofEntries(
                            Map.entry("firstName", "First name must be at most 31 characters."),
                            Map.entry("lastName", LAST_NAME_TOO_SHORT_MSG),
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
        void returnBadRequest_whenLastNameIsTooLong() {
            final var invalidDto = new RegisterRequest(registerRequest.firstName(), TOO_LONG_NAME, registerRequest.userName(),
                    registerRequest.email(), registerRequest.password());

            restClient.post()
                    .body(invalidDto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.details.lastName").isEqualTo("Last name must be at most 31 characters.");
        }

        @Test
        void returnCreated_whenEmailHasMaximumLength() {
            final var dto = new RegisterRequest(registerRequest.firstName(), registerRequest.lastName(), registerRequest.userName(),
                    UserTestData.LONGEST_EMAIL, registerRequest.password());

            restClient.post()
                    .body(dto)
                    .exchange()
                    .expectStatus().isCreated();

            final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS)).send(captor.capture());
            assertThat(captor.getValue().getTo()).containsExactly(UserTestData.LONGEST_EMAIL);
        }

        @Test
        void returnBadRequest_whenEmailIsTooLong() {
            final var invalidDto = new RegisterRequest(registerRequest.firstName(), registerRequest.lastName(), registerRequest.userName(),
                    UserTestData.OVERLONG_EMAIL, registerRequest.password());

            restClient.post()
                    .body(invalidDto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.details.email").isEqualTo(UserTestData.EMAIL_TOO_LONG);
        }

        @Test
        void returnBadRequest_whenUserNameContainsAt() {
            final var invalidDto = new RegisterRequest("Peregrin", "Tuk", "pippin@shire", "pippin@shire.me", "lembasy");

            restClient.post()
                    .body(invalidDto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.details.userName").isEqualTo(USERNAME_CONTAINS_AT_MSG);
        }

        @Test
        void returnBadRequest_whenBodyIsMissing() {
            final var expectedMessage = TestUtils.BODY_UNREADABLE;

            restClient.post()
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMessage);
        }

        @Test
        void returnBadRequest_whenAllFieldsAreNull() {
            registerUser();
            final var invalidDto = new RegisterRequest(null, null, null, null, null);
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
            final var dto = new RegisterRequest("Peregrin", "Tuk", "peregrin", "Pippin@Shire.ME", "lembasy");

            restClient.post()
                    .body(dto)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.details.email").isEqualTo(UserTestData.emailConflictMessage(dto.email()))
                    .jsonPath("$.details.userName").doesNotExist();
        }

        @Test
        void returnConflict_whenUserAlreadyExists() {
            registerUser();
            final var expected = Map.ofEntries(
                    Map.entry("message", UserTestData.USER_ALREADY_EXISTS),
                    Map.entry("details", UserTestData.PIPPIN_CONFLICT_DETAILS));

            restClient.post()
                    .body(registerRequest)
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
                    .headers(auth::admin)
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
                    .headers(auth::admin)
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
                    .headers(auth::admin)
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
                    .headers(auth::admin)
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
                    .headers(auth::user)
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
                    .headers(auth::admin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class).isEqualTo(expectedUserDto);
        }

        @Test
        void returnProvidersWithoutPassword_whenSignedUpWithProvider() {
            userRepository.save(UserTestData.createGooglePippin());

            restClient.get()
                    .uri(ApiConstants.ME)
                    .headers(auth::user)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.hasPassword").isEqualTo(false)
                    .jsonPath("$.providers").isEqualTo(List.of(IdentityProvider.GOOGLE.name()));
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
    class GetCurrentUserStats {

        @Test
        void returnStatsOfOwnFinishedExams_whenAuthenticatedAsUser() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var other = userRepository.save(UserTestData.createMerry());
            examRepository.saveAll(ExamTestData.createExamsForStats(tester));
            examRepository.save(ExamTestData.createFinishedExamWithMaxScore(other));

            restClient.get()
                    .uri(ApiConstants.STATS)
                    .headers(auth::user)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserStatsDto.class).isEqualTo(ExamTestData.STATS);
        }

        @Test
        void returnEmptyStats_whenUserHasNoExams() {
            userRepository.save(UserTestData.createTestUserPippin());

            restClient.get()
                    .uri(ApiConstants.STATS)
                    .headers(auth::user)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserStatsDto.class).isEqualTo(ExamTestData.EMPTY_STATS);
        }

        @Test
        void returnStats_whenAuthenticatedAsAdmin() {
            userRepository.save(UserTestData.createGimli());

            restClient.get()
                    .uri(ApiConstants.STATS)
                    .headers(auth::admin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserStatsDto.class).isEqualTo(ExamTestData.EMPTY_STATS);
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.get()
                    .uri(ApiConstants.STATS)
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
                    .headers(auth::admin)
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
                    .headers(auth::admin)
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
                    .headers(auth::admin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.page.size").isEqualTo(100);
        }

        @Test
        void returnEmptyPage_whenNoUsersExist() {
            restClient.get()
                    .headers(auth::admin)
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
                    .headers(auth::admin)
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
        void returnConflict_whenUserIsUpdatedConcurrently() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var id = tester.getId();
            final var request = UserTestData.createUserUpdateRequestWithoutEmail();
            final var concurrentFirstName = "Paladin";
            final var realMethod = mockingDetails(userRepository).getMockCreationSettings().getDefaultAnswer();
            doAnswer(invocation -> {
                final var exists = realMethod.answer(invocation);
                restClient.patch()
                        .uri(ApiConstants.BY_ID, id)
                        .headers(auth::user)
                        .body(new UserUpdateRequest(concurrentFirstName, null, null, null))
                        .exchange()
                        .expectStatus().isOk();
                return exists;
            }).when(userRepository).existsByUserNameIgnoreCaseAndIdNot(request.userName(), id);

            restClient.patch()
                    .uri(ApiConstants.BY_ID, id)
                    .headers(auth::user)
                    .body(request)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.CONCURRENT_MODIFICATION);

            final var stored = userRepository.findById(id).orElseThrow();
            assertThat(stored.getFirstName()).isEqualTo(concurrentFirstName);
            assertThat(stored.getUserName()).isEqualTo(tester.getUserName());
        }

        @Test
        void success_whenAdminUpdatesAnotherUserWithoutEmail() {
            final var id = userRepository.save(UserTestData.createTestUserPippin()).getId();
            final var validDto = UserTestData.createUserUpdateRequestWithoutEmail();
            final var expected = UserTestData.createUpdatedByAdminUserDto(id);

            restClient.patch()
                    .uri(ApiConstants.BY_ID, id)
                    .headers(auth::admin)
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
                    .headers(auth::user)
                    .body(validDto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class).isEqualTo(expected);
        }

        @Test
        void success_whenUserChangesCaseOfOwnUserName() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var userName = tester.getUserName().toUpperCase(Locale.ROOT);

            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(auth::user)
                    .body(new UserUpdateRequest(null, null, userName, null))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class).isEqualTo(UserTestData.createUserDto(tester.getId(), userName));

            assertThat(userRepository.findById(tester.getId()).orElseThrow().getUserName()).isEqualTo(userName);
        }

        @Test
        void returnConflict_whenUserNameDiffersOnlyInCaseFromAnotherUser() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var merry = userRepository.save(UserTestData.createMerry());
            final var userName = merry.getUserName().toUpperCase(Locale.ROOT);

            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(auth::user)
                    .body(new UserUpdateRequest(null, null, userName, null))
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.USER_ALREADY_EXISTS)
                    .jsonPath("$.details.userName").isEqualTo(UserTestData.userNameConflictMessage(userName));

            assertThat(userRepository.findById(tester.getId()).orElseThrow().getUserName())
                    .isEqualTo(tester.getUserName());
        }

        @Test
        void keepEmailAndSendLinksToNewAndCurrentEmail_whenEmailChanged() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var newEmail = UserTestData.createValidUserUpdateRequest().email();

            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(auth::user)
                    .body(new UserUpdateRequest(null, null, null, newEmail))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.email").isEqualTo(tester.getEmail())
                    .jsonPath("$.pendingEmail").isEqualTo(UserTestData.STORED_UPDATED_EMAIL)
                    .jsonPath("$.awaitingNewEmailConfirmation").isEqualTo(true)
                    .jsonPath("$.awaitingCurrentEmailConfirmation").isEqualTo(true);

            final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS).times(2)).send(captor.capture());
            final var link = captor.getAllValues().getFirst();
            final var confirmation = captor.getAllValues().getLast();
            assertThat(link.getTo()).containsExactly(UserTestData.STORED_UPDATED_EMAIL);
            assertThat(confirmation.getTo()).containsExactly(tester.getEmail());
            final var updated = userRepository.findById(tester.getId()).orElseThrow();
            assertThat(updated.getEmail()).isEqualTo(tester.getEmail());
            assertThat(updated.getPendingEmail()).isEqualTo(UserTestData.STORED_UPDATED_EMAIL);
            assertThat(updated.getVerificationTokenHash())
                    .isEqualTo(SecureTokens.hash(UserTestData.extractToken(link)));
            assertThat(updated.getCurrentEmailTokenHash())
                    .isEqualTo(SecureTokens.hash(UserTestData.extractToken(confirmation)));
        }

        @Test
        void returnForbiddenAndSendNoEmail_whenAdminChangesAnotherUsersEmail() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());

            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(auth::admin)
                    .body(UserTestData.createValidUserUpdateRequest())
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.ACCESS_DENIED);

            final var unchanged = userRepository.findById(tester.getId()).orElseThrow();
            assertThat(unchanged.getUserName()).isEqualTo(tester.getUserName());
            assertThat(unchanged.getPendingEmail()).isNull();
            verifyNoMoreInteractions(mailSender);
        }

        @Test
        void returnForbidden_whenAdminCancelsAnotherUsersEmailChange() {
            final var tester = userRepository.save(UserTestData.createPippinChangingEmail());

            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(auth::admin)
                    .body(new UserUpdateRequest(null, null, null, tester.getEmail()))
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.ACCESS_DENIED);

            assertThat(userRepository.findById(tester.getId())
                    .orElseThrow()
                    .getPendingEmail()).isEqualTo(tester.getPendingEmail());
        }

        @Test
        void returnTooManyRequestsAndKeepPendingEmail_whenEmailChangedAgainWithinCooldown() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var firstEmail = UserTestData.createValidUserUpdateRequest().email();
            final var secondEmail = UserTestData.createMerry().getEmail();
            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(auth::user)
                    .body(new UserUpdateRequest(null, null, null, firstEmail))
                    .exchange()
                    .expectStatus().isOk();
            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS).times(2)).send(any(SimpleMailMessage.class));

            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(auth::user)
                    .body(new UserUpdateRequest(null, null, null, secondEmail))
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.VERIFICATION_COOLDOWN);

            assertThat(userRepository.findById(tester.getId()).orElseThrow().getPendingEmail())
                    .isEqualTo(UserTestData.STORED_UPDATED_EMAIL);
            verifyNoMoreInteractions(mailSender);
        }

        @Test
        void cancelEmailChangeWithinCooldown_whenCurrentEmailSent() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var newEmail = UserTestData.createValidUserUpdateRequest().email();
            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(auth::user)
                    .body(new UserUpdateRequest(null, null, null, newEmail))
                    .exchange()
                    .expectStatus().isOk();
            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS).times(2)).send(any(SimpleMailMessage.class));

            restClient.patch()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(auth::user)
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
                        .headers(auth::user)
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
                    .headers(auth::user)
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
                            Map.entry("lastName", LAST_NAME_TOO_SHORT_MSG),
                            Map.entry("email", "Email format is not valid."))));

            restClient.patch()
                    .uri(ApiConstants.BY_ID, 1L)
                    .headers(auth::user)
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
                    .headers(auth::user)
                    .body(invalidUpdateRequest)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.details.userName").isEqualTo(USERNAME_CONTAINS_AT_MSG);
        }

        @Test
        void returnBadRequest_whenEmailIsTooShort() {
            final var invalidUpdateRequest = new UserUpdateRequest(null, null, null, "p@sh");

            restClient.patch()
                    .uri(ApiConstants.BY_ID, 1L)
                    .headers(auth::user)
                    .body(invalidUpdateRequest)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.details.email").isEqualTo("Email must be at least 5 characters.");
        }

        @Test
        void success_whenEmailHasMaximumLength() {
            final var id = userRepository.save(UserTestData.createTestUserPippin()).getId();
            final var request = new UserUpdateRequest(null, null, null, UserTestData.LONGEST_EMAIL);

            restClient.patch()
                    .uri(ApiConstants.BY_ID, id)
                    .headers(auth::user)
                    .body(request)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.pendingEmail").isEqualTo(UserTestData.LONGEST_EMAIL);

            final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS).times(2)).send(captor.capture());
            assertThat(captor.getAllValues().getFirst().getTo()).containsExactly(UserTestData.LONGEST_EMAIL);
        }

        @Test
        void returnBadRequest_whenEmailIsTooLong() {
            final var invalidUpdateRequest = new UserUpdateRequest(null, null, null, UserTestData.OVERLONG_EMAIL);

            restClient.patch()
                    .uri(ApiConstants.BY_ID, 1L)
                    .headers(auth::user)
                    .body(invalidUpdateRequest)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.details.email").isEqualTo(UserTestData.EMAIL_TOO_LONG);
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, 0L})
        void returnBadRequest_whenIdIsNotPositive(final long invalidId) {
            final var validDto = UserTestData.createUserUpdateRequestWithoutEmail();
            final var expectedMessage = TestUtils.ID_NOT_POSITIVE;

            restClient.patch()
                    .uri(ApiConstants.BY_ID, invalidId)
                    .headers(auth::admin)
                    .body(validDto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMessage);
        }

        @Test
        void returnNotFound_whenUserDoesNotExist() {
            final var nonExistentId = 666L;
            final var validDto = UserTestData.createUserUpdateRequestWithoutEmail();
            final var expectedMessage = UserTestData.userNotFoundMessage(nonExistentId);

            restClient.patch()
                    .uri(ApiConstants.BY_ID, nonExistentId)
                    .headers(auth::admin)
                    .body(validDto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMessage);
        }

        @Test
        void returnBadRequest_whenBodyIsMissing() {
            final var expectedMessage = TestUtils.BODY_UNREADABLE;

            restClient.patch()
                    .uri(ApiConstants.BY_ID, 1L)
                    .headers(auth::user)
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
                    .headers(auth::user)
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
                    .headers(auth::user)
                    .body(UserTestData.createValidChangePasswordRequest())
                    .exchange()
                    .expectStatus().isNoContent();

            final var updated = userRepository.findById(tester.getId()).orElseThrow();
            assertThat(updated.getEmail()).isEqualTo(tester.getEmail());
            assertThat(updated.getPendingEmail()).isNull();
            assertThat(updated.getVerificationTokenHash()).isNull();
            assertThat(updated.getCurrentEmailTokenHash()).isNull();
        }

        @Test
        void revokeOnlyOwnRefreshTokens_whenRequestIsValid() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var expires = Instant.now().plus(RefreshTokenTestData.VALIDITY);
            refreshTokenRepository.save(
                    RefreshTokenTestData.createRefreshToken(tester, RefreshTokenTestData.TOKEN, expires));
            final var merry = userRepository.save(UserTestData.createMerry());
            final var othersToken = refreshTokenRepository.save(
                    RefreshTokenTestData.createRefreshToken(merry, "othersToken", expires));

            restClient.patch()
                    .uri(ApiConstants.PASSWORD)
                    .headers(auth::user)
                    .body(UserTestData.createValidChangePasswordRequest())
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(refreshTokenRepository.findAll()).containsExactly(othersToken);
        }

        @Test
        void changeOnlyOwnPassword_whenOtherUsersExist() {
            registerUser();
            final var other = userRepository.save(UserTestData.createMerry());
            final var request = UserTestData.createValidChangePasswordRequest();

            restClient.patch()
                    .uri(ApiConstants.PASSWORD)
                    .headers(auth::user)
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
                    .headers(auth::user)
                    .body(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.INCORRECT_PASSWORD);
        }

        @Test
        void returnBadRequest_whenNewPasswordIsSameAsCurrent() {
            final var id = registerUser();
            final var request = UserTestData.createSameAsCurrentPasswordRequest();

            restClient.patch()
                    .uri(ApiConstants.PASSWORD)
                    .headers(auth::user)
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
                    .headers(auth::user)
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
                    .headers(auth::user)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.BODY_UNREADABLE);
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
    class DeleteOwnAccount {

        @Test
        void returnNoContentAndDeleteOnlyOwnAccount_whenPasswordIsCorrect() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var other = userRepository.save(UserTestData.createMerry());

            restClient.method(HttpMethod.DELETE)
                    .uri(ApiConstants.ME)
                    .headers(auth::user)
                    .body(UserTestData.createValidDeleteAccountRequest())
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(userRepository.existsById(tester.getId())).isFalse();
            assertThat(userRepository.existsById(other.getId())).isTrue();
        }

        @Test
        void removeOwnExamsButKeepQuestionsAndOtherUsersExams_whenPasswordIsCorrect() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var other = userRepository.save(UserTestData.createMerry());
            final var question = questionRepository.save(QuestionTestData.createQuestion(QuestionType.SPECIAL));
            examRepository.save(ExamTestData.createExamWithoutQuestions(tester)
                    .setQuestions(List.of(question))
                    .setUserAnswers(List.of(question.getAnswers().getFirst())));
            final var otherExam = examRepository.save(ExamTestData.createExamWithoutQuestions(other)
                    .setQuestions(List.of(question)));

            restClient.method(HttpMethod.DELETE)
                    .uri(ApiConstants.ME)
                    .headers(auth::user)
                    .body(UserTestData.createValidDeleteAccountRequest())
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(examRepository.findAll()).containsExactly(otherExam);
            assertThat(questionRepository.existsById(question.getId())).isTrue();
        }

        @Test
        void allowRegisteringSameUserNameAndEmailAgain_whenAccountDeleted() {
            userRepository.save(UserTestData.createTestUserPippin());
            restClient.method(HttpMethod.DELETE)
                    .uri(ApiConstants.ME)
                    .headers(auth::user)
                    .body(UserTestData.createValidDeleteAccountRequest())
                    .exchange()
                    .expectStatus().isNoContent();

            restClient.post()
                    .body(registerRequest)
                    .exchange()
                    .expectStatus().isCreated();

            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS)).send(any(SimpleMailMessage.class));
            assertThat(userRepository.existsByUserNameIgnoreCase(registerRequest.userName())).isTrue();
            assertThat(userRepository.existsByEmailIgnoreCase(registerRequest.email())).isTrue();
        }

        @Test
        void returnNoContent_whenAuthenticatedAsAdmin() {
            final var admin = userRepository.save(UserTestData.createGimli());

            restClient.method(HttpMethod.DELETE)
                    .uri(ApiConstants.ME)
                    .headers(auth::admin)
                    .body(new DeleteAccountRequest(TestUtils.ADMIN_PASSWORD))
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(userRepository.existsById(admin.getId())).isFalse();
        }

        @Test
        void returnBadRequestAndKeepAccount_whenPasswordIsIncorrect() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());

            restClient.method(HttpMethod.DELETE)
                    .uri(ApiConstants.ME)
                    .headers(auth::user)
                    .body(UserTestData.createWrongPasswordDeleteAccountRequest())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.INCORRECT_PASSWORD);

            assertThat(userRepository.existsById(tester.getId())).isTrue();
        }

        @Test
        void returnNoContent_whenPasswordlessUserSetsPasswordThroughReset() {
            final var tester = userRepository.save(UserTestData.createGooglePippin());
            final var authClient = TestUtils.createRestTestClient(port, ApiConstants.AUTH_BASE_URL);

            restClient.method(HttpMethod.DELETE)
                    .uri(ApiConstants.ME)
                    .headers(auth::user)
                    .body(UserTestData.createValidDeleteAccountRequest())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.INCORRECT_PASSWORD);

            authClient.post()
                    .uri(ApiConstants.FORGOT_PASSWORD)
                    .body(new ForgotPasswordRequest(tester.getEmail()))
                    .exchange()
                    .expectStatus().isAccepted();
            final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS)).send(captor.capture());
            final var reset = UserTestData.createValidResetPasswordRequest(UserTestData.extractToken(captor.getValue()));
            authClient.post()
                    .uri(ApiConstants.RESET_PASSWORD)
                    .body(reset)
                    .exchange()
                    .expectStatus().isNoContent();

            restClient.method(HttpMethod.DELETE)
                    .uri(ApiConstants.ME)
                    .headers(auth::user)
                    .body(new DeleteAccountRequest(reset.newPassword()))
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(userRepository.existsById(tester.getId())).isFalse();
        }

        @Test
        void returnBadRequest_whenPasswordIsBlank() {
            restClient.method(HttpMethod.DELETE)
                    .uri(ApiConstants.ME)
                    .headers(auth::user)
                    .body(new DeleteAccountRequest(" "))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.VALIDATION_FAILED)
                    .jsonPath("$.details.password").isEqualTo(TestUtils.PASSWORD_REQUIRED);
        }

        @Test
        void returnBadRequest_whenBodyIsMissing() {
            restClient.delete()
                    .uri(ApiConstants.ME)
                    .headers(auth::user)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.BODY_UNREADABLE);
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.method(HttpMethod.DELETE)
                    .uri(ApiConstants.ME)
                    .body(UserTestData.createValidDeleteAccountRequest())
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

    }

    @Nested
    class LinkGoogle {

        private final String idToken = GoogleTestData.idToken(GoogleTestData.createPippinClaims());

        @Test
        void returnNoContentAndAllowSignIn_whenEmailDiffers() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin()
                    .setEmail(UserTestData.createMerry().getEmail()));

            link(idToken)
                    .expectStatus().isNoContent();

            final var tokens = signIn(ApiConstants.GOOGLE, new GoogleSignInRequest(idToken));
            assertThat(tokenOwnerId(tokens)).isEqualTo(tester.getId());
            assertThat(userRepository.count()).isOne();
            assertNoticeSent(tester, UserTestData.identityLinkedSubject(IdentityProvider.GOOGLE));
        }

        @Test
        void returnNoContent_whenAdmin() {
            final var admin = userRepository.save(UserTestData.createGimli());

            restClient.post()
                    .uri(ApiConstants.IDENTITIES + ApiConstants.GOOGLE)
                    .headers(auth::admin)
                    .body(new LinkGoogleRequest(idToken, TestUtils.ADMIN_PASSWORD))
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID))
                    .get().extracting(User::getId).isEqualTo(admin.getId());
            assertNoticeSent(admin, UserTestData.identityLinkedSubject(IdentityProvider.GOOGLE));
        }

        @Test
        void returnConflict_whenAccountIsLinkedToOtherUser() {
            final var merry = userRepository.save(UserTestData.createMerry()
                    .addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID));
            userRepository.save(UserTestData.createTestUserPippin());

            link(idToken)
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(GoogleTestData.LINKED_TO_OTHER_USER)
                    .jsonPath("$.details.provider").isEqualTo(GoogleTestData.LINKED_TO_OTHER_USER);

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID))
                    .get().extracting(User::getId).isEqualTo(merry.getId());
        }

        @Test
        void returnConflict_whenUserHasGoogleLinked() {
            userRepository.save(UserTestData.createTestUserPippin()
                    .addIdentity(IdentityProvider.GOOGLE, UserTestData.OTHER_PROVIDER_USER_ID));

            link(idToken)
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(GoogleTestData.ALREADY_LINKED)
                    .jsonPath("$.details.provider").isEqualTo(GoogleTestData.ALREADY_LINKED);

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)).isEmpty();
        }

        @Test
        void returnUnauthorizedAndNotLink_whenIdTokenIsInvalid() {
            userRepository.save(UserTestData.createTestUserPippin());

            link("not-a-jwt")
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(GoogleTestData.INVALID_ID_TOKEN);

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)).isEmpty();
        }

        @Test
        void returnNoContent_whenUserHasNoPassword() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin()
                    .setPassword(null)
                    .addIdentity(IdentityProvider.GITHUB, UserTestData.OTHER_PROVIDER_USER_ID));

            link(idToken, null)
                    .expectStatus().isNoContent();

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID))
                    .get().extracting(User::getId).isEqualTo(tester.getId());
            assertNoticeSent(tester, UserTestData.identityLinkedSubject(IdentityProvider.GOOGLE));
        }

        @Test
        void returnServiceUnavailableAndNotLink_whenTaskQueueIsFull() {
            userRepository.save(UserTestData.createTestUserPippin());

            final var release = TestUtils.blockTaskExecutor(taskExecutor);
            try {
                link(idToken)
                        .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                        .expectBody()
                        .jsonPath("$.detail").isEqualTo(TestUtils.SERVER_BUSY);
            } finally {
                release.countDown();
            }

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)).isEmpty();
        }

        @Test
        void returnBadRequestAndNotLink_whenPasswordIsIncorrect() {
            userRepository.save(UserTestData.createTestUserPippin());

            link(idToken, UserTestData.WRONG_PASSWORD)
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.INCORRECT_PASSWORD);

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)).isEmpty();
        }

        @Test
        void returnBadRequestAndNotLink_whenPasswordIsMissing() {
            userRepository.save(UserTestData.createTestUserPippin());

            link(idToken, null)
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.INCORRECT_PASSWORD);

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)).isEmpty();
        }

        @Test
        void returnBadRequest_whenBodyIsMissing() {
            userRepository.save(UserTestData.createTestUserPippin());

            restClient.post()
                    .uri(ApiConstants.IDENTITIES + ApiConstants.GOOGLE)
                    .headers(auth::user)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.BODY_UNREADABLE);
        }

        @Test
        void returnBadRequest_whenIdTokenIsBlank() {
            userRepository.save(UserTestData.createTestUserPippin());

            link(" ")
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.VALIDATION_FAILED);
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.post()
                    .uri(ApiConstants.IDENTITIES + ApiConstants.GOOGLE)
                    .body(new LinkGoogleRequest(idToken, TestUtils.USER_PASSWORD))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        private RestTestClient.ResponseSpec link(final String token) {
            return link(token, TestUtils.USER_PASSWORD);
        }

        private RestTestClient.ResponseSpec link(final String token, @Nullable final String currentPassword) {
            return restClient.post()
                    .uri(ApiConstants.IDENTITIES + ApiConstants.GOOGLE)
                    .headers(auth::user)
                    .body(new LinkGoogleRequest(token, currentPassword))
                    .exchange();
        }

    }

    @Nested
    class LinkGitHub {

        private final GitHubSignInRequest request = GitHubTestData.createValidRequest();

        @Test
        void returnNoContentAndAllowSignIn_whenEmailDiffers() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin()
                    .setEmail(UserTestData.createMerry().getEmail()));
            GitHubTestData.expectSignIn(github);
            GitHubTestData.expectSignIn(github);

            link()
                    .expectStatus().isNoContent();

            final var tokens = signIn(ApiConstants.GITHUB, request);
            assertThat(tokenOwnerId(tokens)).isEqualTo(tester.getId());
            assertThat(userRepository.count()).isOne();
            github.verify();
            assertNoticeSent(tester, UserTestData.identityLinkedSubject(IdentityProvider.GITHUB));
        }

        @Test
        void returnNoContent_whenPrimaryEmailIsNotVerified() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            GitHubTestData.expectSignInWithUnverifiedEmail(github);

            link()
                    .expectStatus().isNoContent();

            assertThat(userRepository.findByIdentity(IdentityProvider.GITHUB, UserTestData.PROVIDER_USER_ID))
                    .get().extracting(User::getId).isEqualTo(tester.getId());
            assertNoticeSent(tester, UserTestData.identityLinkedSubject(IdentityProvider.GITHUB));
        }

        @Test
        void returnConflict_whenUserHasGitHubLinked() {
            userRepository.save(UserTestData.createTestUserPippin()
                    .addIdentity(IdentityProvider.GITHUB, UserTestData.OTHER_PROVIDER_USER_ID));
            GitHubTestData.expectSignIn(github);

            link()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(GitHubTestData.ALREADY_LINKED);

            assertThat(userRepository.findByIdentity(IdentityProvider.GITHUB, UserTestData.PROVIDER_USER_ID)).isEmpty();
        }

        @Test
        void returnUnauthorizedAndNotLink_whenCodeIsRejected() {
            userRepository.save(UserTestData.createTestUserPippin());
            GitHubTestData.expectTokenExchange(github, GitHubTestData.json(GitHubTestData.BAD_CODE_RESPONSE));

            link()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(GitHubTestData.INVALID_CODE);

            assertThat(userRepository.findByIdentity(IdentityProvider.GITHUB, UserTestData.PROVIDER_USER_ID)).isEmpty();
        }

        @Test
        void returnForbiddenAndNotLink_whenExchangeRefusesUnverifiedEmail() {
            userRepository.save(UserTestData.createTestUserPippin());
            GitHubTestData.expectTokenExchange(github, GitHubTestData.json(GitHubTestData.UNVERIFIED_EMAIL_RESPONSE));

            link()
                    .expectStatus().isForbidden()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(GitHubTestData.EMAIL_NOT_VERIFIED);

            assertThat(userRepository.findByIdentity(IdentityProvider.GITHUB, UserTestData.PROVIDER_USER_ID)).isEmpty();
        }

        @Test
        void returnNoContent_whenUserHasNoPassword() {
            final var tester = userRepository.save(UserTestData.createGooglePippin());
            GitHubTestData.expectSignIn(github);

            link(GitHubTestData.createLinkRequest(null))
                    .expectStatus().isNoContent();

            assertThat(userRepository.findByIdentity(IdentityProvider.GITHUB, UserTestData.PROVIDER_USER_ID))
                    .get().extracting(User::getId).isEqualTo(tester.getId());
            github.verify();
            assertNoticeSent(tester, UserTestData.identityLinkedSubject(IdentityProvider.GITHUB));
        }

        // No GitHub requests are expected in these two, so one would fail the test: the code isn't used up.
        @Test
        void returnBadRequestAndNotCallGitHub_whenPasswordIsIncorrect() {
            userRepository.save(UserTestData.createTestUserPippin());

            link(GitHubTestData.createLinkRequest(UserTestData.WRONG_PASSWORD))
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.INCORRECT_PASSWORD);

            assertThat(userRepository.findByIdentity(IdentityProvider.GITHUB, UserTestData.PROVIDER_USER_ID)).isEmpty();
        }

        @Test
        void returnBadRequestAndNotCallGitHub_whenPasswordIsMissing() {
            userRepository.save(UserTestData.createTestUserPippin());

            link(GitHubTestData.createLinkRequest(null))
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.INCORRECT_PASSWORD);

            assertThat(userRepository.findByIdentity(IdentityProvider.GITHUB, UserTestData.PROVIDER_USER_ID)).isEmpty();
        }

        @Test
        void returnBadRequest_whenBodyIsMissing() {
            userRepository.save(UserTestData.createTestUserPippin());

            restClient.post()
                    .uri(ApiConstants.IDENTITIES + ApiConstants.GITHUB)
                    .headers(auth::user)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.BODY_UNREADABLE);
        }

        @Test
        void returnBadRequest_whenFieldsAreBlank() {
            userRepository.save(UserTestData.createTestUserPippin());

            restClient.post()
                    .uri(ApiConstants.IDENTITIES + ApiConstants.GITHUB)
                    .headers(auth::user)
                    .body(new LinkGitHubRequest(" ", " ", " ", TestUtils.USER_PASSWORD))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.VALIDATION_FAILED);
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.post()
                    .uri(ApiConstants.IDENTITIES + ApiConstants.GITHUB)
                    .body(GitHubTestData.createValidLinkRequest())
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        private RestTestClient.ResponseSpec link() {
            return link(GitHubTestData.createValidLinkRequest());
        }

        private RestTestClient.ResponseSpec link(final LinkGitHubRequest linkRequest) {
            return restClient.post()
                    .uri(ApiConstants.IDENTITIES + ApiConstants.GITHUB)
                    .headers(auth::user)
                    .body(linkRequest)
                    .exchange();
        }

    }

    @Nested
    class UnlinkIdentity {

        private final String notHostedIdToken =
                GoogleTestData.idToken(GoogleTestData.createPippinClaimsWithoutHostedDomain());

        @Test
        void returnNoContentAndBlockSignIn_whenUnlinkingGoogle() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin()
                    .addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID));
            final var request = new GoogleSignInRequest(notHostedIdToken);
            assertThat(tokenOwnerId(signIn(ApiConstants.GOOGLE, request))).isEqualTo(tester.getId());

            unlink(ApiConstants.GOOGLE)
                    .expectStatus().isNoContent();

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)).isEmpty();
            attemptSignIn(ApiConstants.GOOGLE, request)
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(GoogleTestData.EMAIL_NOT_HOSTED);
            assertNoticeSent(tester, UserTestData.identityUnlinkedSubject(IdentityProvider.GOOGLE));
        }

        @Test
        void returnNoContentAndBlockSignIn_whenUnlinkingGitHub() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin()
                    .addIdentity(IdentityProvider.GITHUB, UserTestData.PROVIDER_USER_ID));
            GitHubTestData.expectSignIn(github);

            unlink(ApiConstants.GITHUB)
                    .expectStatus().isNoContent();

            assertThat(userRepository.findByIdentity(IdentityProvider.GITHUB, UserTestData.PROVIDER_USER_ID)).isEmpty();
            attemptSignIn(ApiConstants.GITHUB, GitHubTestData.createValidRequest())
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(GitHubTestData.EMAIL_NOT_HOSTED);
            github.verify();
            assertNoticeSent(tester, UserTestData.identityUnlinkedSubject(IdentityProvider.GITHUB));
        }

        @Test
        void linkAgainOnNextSignIn_whenProviderHostsEmail() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin()
                    .addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID));

            unlink(ApiConstants.GOOGLE)
                    .expectStatus().isNoContent();

            final var idToken = GoogleTestData.idToken(GoogleTestData.createPippinClaims());
            final var tokens = signIn(ApiConstants.GOOGLE, new GoogleSignInRequest(idToken));
            assertThat(tokenOwnerId(tokens)).isEqualTo(tester.getId());
            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID))
                    .isPresent();
            assertNoticeSent(tester, UserTestData.identityUnlinkedSubject(IdentityProvider.GOOGLE));
        }

        @Test
        void returnNoContentAndKeepOtherIdentity_whenPasswordlessUserHasAnother() {
            final var tester = userRepository.save(UserTestData.createGooglePippin()
                    .addIdentity(IdentityProvider.GITHUB, UserTestData.OTHER_PROVIDER_USER_ID));

            unlinkWithoutBody(ApiConstants.GOOGLE)
                    .expectStatus().isNoContent();

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)).isEmpty();
            assertThat(userRepository.findByIdentity(IdentityProvider.GITHUB, UserTestData.OTHER_PROVIDER_USER_ID))
                    .isPresent();
            assertNoticeSent(tester, UserTestData.identityUnlinkedSubject(IdentityProvider.GOOGLE));
        }

        @Test
        void returnNoContent_whenAdmin() {
            final var admin = userRepository.save(UserTestData.createGimli()
                    .addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID));

            restClient.method(HttpMethod.DELETE)
                    .uri(ApiConstants.IDENTITIES + ApiConstants.GOOGLE)
                    .headers(auth::admin)
                    .body(new UnlinkIdentityRequest(TestUtils.ADMIN_PASSWORD))
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)).isEmpty();
            assertNoticeSent(admin, UserTestData.identityUnlinkedSubject(IdentityProvider.GOOGLE));
        }

        @Test
        void returnConflictAndKeepIdentity_whenItIsTheLastSignInMethod() {
            userRepository.save(UserTestData.createGooglePippin());

            unlinkWithoutBody(ApiConstants.GOOGLE)
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.LAST_SIGN_IN_METHOD);

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID))
                    .isPresent();
        }

        @Test
        void keepOneIdentity_whenPasswordlessUserUnlinksBothConcurrently() {
            userRepository.save(UserTestData.createGooglePippin()
                    .addIdentity(IdentityProvider.GITHUB, UserTestData.OTHER_PROVIDER_USER_ID));

            final var statuses = Stream.of(ApiConstants.GOOGLE, ApiConstants.GITHUB)
                    .parallel()
                    .map(provider -> unlinkWithoutBody(provider)
                            .returnResult(Void.class)
                            .getStatus()
                            .value())
                    .toList();

            assertThat(statuses)
                    .containsExactlyInAnyOrder(HttpStatus.NO_CONTENT.value(), HttpStatus.CONFLICT.value());
            assertThat(Stream.of(
                            userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID),
                            userRepository.findByIdentity(IdentityProvider.GITHUB, UserTestData.OTHER_PROVIDER_USER_ID))
                    .flatMap(Optional::stream))
                    .hasSize(1);
            verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS)).send(any(SimpleMailMessage.class));
        }

        @Test
        void returnServiceUnavailableAndKeepIdentity_whenTaskQueueIsFull() {
            userRepository.save(UserTestData.createTestUserPippin()
                    .addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID));

            final var release = TestUtils.blockTaskExecutor(taskExecutor);
            try {
                unlink(ApiConstants.GOOGLE)
                        .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                        .expectBody()
                        .jsonPath("$.detail").isEqualTo(TestUtils.SERVER_BUSY);
            } finally {
                release.countDown();
            }

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)).isPresent();
        }

        @Test
        void returnNotFound_whenGoogleIsNotLinked() {
            userRepository.save(UserTestData.createTestUserPippin()
                    .addIdentity(IdentityProvider.GITHUB, UserTestData.PROVIDER_USER_ID));

            unlink(ApiConstants.GOOGLE)
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(GoogleTestData.NOT_LINKED);
        }

        @Test
        void returnNotFound_whenGitHubIsNotLinked() {
            userRepository.save(UserTestData.createTestUserPippin()
                    .addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID));

            unlink(ApiConstants.GITHUB)
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(GitHubTestData.NOT_LINKED);
        }

        @ParameterizedTest
        @ValueSource(strings = {ApiConstants.GOOGLE, ApiConstants.GITHUB})
        void returnBadRequestAndKeepIdentities_whenPasswordIsIncorrect(final String provider) {
            userRepository.save(UserTestData.createTestUserPippin()
                    .addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)
                    .addIdentity(IdentityProvider.GITHUB, UserTestData.OTHER_PROVIDER_USER_ID));

            unlink(provider, UserTestData.WRONG_PASSWORD)
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.INCORRECT_PASSWORD);

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)).isPresent();
            assertThat(userRepository.findByIdentity(IdentityProvider.GITHUB, UserTestData.OTHER_PROVIDER_USER_ID))
                    .isPresent();
        }

        @ParameterizedTest
        @ValueSource(strings = {ApiConstants.GOOGLE, ApiConstants.GITHUB})
        void returnBadRequestAndKeepIdentities_whenBodyIsMissing(final String provider) {
            userRepository.save(UserTestData.createTestUserPippin()
                    .addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)
                    .addIdentity(IdentityProvider.GITHUB, UserTestData.OTHER_PROVIDER_USER_ID));

            unlinkWithoutBody(provider)
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.INCORRECT_PASSWORD);

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)).isPresent();
            assertThat(userRepository.findByIdentity(IdentityProvider.GITHUB, UserTestData.OTHER_PROVIDER_USER_ID))
                    .isPresent();
        }

        @Test
        void returnBadRequestAndKeepIdentity_whenPasswordIsNull() {
            userRepository.save(UserTestData.createTestUserPippin()
                    .addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID));

            unlink(ApiConstants.GOOGLE, null)
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.INCORRECT_PASSWORD);

            assertThat(userRepository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)).isPresent();
        }

        @ParameterizedTest
        @ValueSource(strings = {ApiConstants.GOOGLE, ApiConstants.GITHUB})
        void returnUnauthorized_whenNotAuthenticated(final String provider) {
            restClient.delete()
                    .uri(ApiConstants.IDENTITIES + provider)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        private RestTestClient.ResponseSpec unlink(final String provider) {
            return unlink(provider, TestUtils.USER_PASSWORD);
        }

        private RestTestClient.ResponseSpec unlink(final String provider, @Nullable final String currentPassword) {
            return restClient.method(HttpMethod.DELETE)
                    .uri(ApiConstants.IDENTITIES + provider)
                    .headers(auth::user)
                    .body(new UnlinkIdentityRequest(currentPassword))
                    .exchange();
        }

        private RestTestClient.ResponseSpec unlinkWithoutBody(final String provider) {
            return restClient.delete()
                    .uri(ApiConstants.IDENTITIES + provider)
                    .headers(auth::user)
                    .exchange();
        }

    }

    @Nested
    class DeleteUser {

        @Test
        void returnNoContentAndRemoveUserWithExams_whenSuccess() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            examRepository.save(ExamTestData.createExamWithoutQuestions(tester));

            restClient.delete()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(auth::admin)
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(userRepository.existsById(tester.getId())).isFalse();
            assertThat(examRepository.findAll()).isEmpty();
        }

        @Test
        void returnForbiddenAndKeepUser_whenAuthenticatedAsUser() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());

            restClient.delete()
                    .uri(ApiConstants.BY_ID, tester.getId())
                    .headers(auth::user)
                    .exchange()
                    .expectStatus().isForbidden();

            assertThat(userRepository.existsById(tester.getId())).isTrue();
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
                    .headers(auth::admin)
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
                    .headers(auth::admin)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMessage);
        }

    }

    private long registerUser() {
        restClient.post()
                .body(registerRequest)
                .exchangeSuccessfully();
        return userRepository.findAll().getFirst().getId();
    }

    /**
     * Also waits for the notice to be sent, so that it can't reach the shared mock during the next test.
     */
    private void assertNoticeSent(final User user, final String subject) {
        final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(TestUtils.ASYNC_TIMEOUT_MS)).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly(user.getEmail());
        assertThat(captor.getValue().getSubject()).isEqualTo(subject);
    }

    private RestTestClient.ResponseSpec attemptSignIn(final String path, final Object request) {
        return TestUtils.createRestTestClient(port, ApiConstants.AUTH_BASE_URL)
                .post()
                .uri(path)
                .body(request)
                .exchange();
    }

    private AuthTokensDto signIn(final String path, final Object request) {
        return attemptSignIn(path, request)
                .expectStatus().isOk()
                .expectBody(AuthTokensDto.class)
                .returnResult()
                .getResponseBody();
    }

    private long tokenOwnerId(final AuthTokensDto tokens) {
        return refreshTokenRepository.findByTokenHash(SecureTokens.hash(tokens.refreshToken()))
                .orElseThrow()
                .getUser()
                .getId();
    }

}
