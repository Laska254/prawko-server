package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.test_data.TestDataFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class UserControllerTest {

    @Autowired
    private UserRepository userRepository;

    @LocalServerPort
    private int port;

    private RestTestClient restClient;

    private final TestDataFactory testDataFactory = new TestDataFactory();
    private final RegisterDto registerDto = testDataFactory.createValidRegisterDto();

    @BeforeEach
    void setUp() {
        restClient = TestUtils.createRestTestClient(port, ApiConstants.USERS_BASE_URL);
    }

    @Nested
    class WithRegisteredUser {

        @BeforeEach
        public void registerUser() {
            restClient.post()
                    .body(registerDto)
                    .exchangeSuccessfully();
        }

        @Test
        void registerUser_returnConflict_whenUserAlreadyExists() {
            final var expected = Map.of(
                    "message", "User already exists.",
                    "details", Map.of(
                            "email", "User with email 'pippin@shire.me' already exists.",
                            "userName", "User with username 'pippin' already exists."));

            restClient.post()
                    .body(registerDto)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectBody(Map.class).isEqualTo(expected);
        }

        @Test
        void getUserById_returnUserDto_whenFound() {
            final var expectedUserDto = testDataFactory.createUserDto();

            restClient.get()
                    .uri(ApiConstants.BY_ID, 1L)
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class).isEqualTo(expectedUserDto);
        }

        @Test
        void getUserById_returnUnauthorized_whenNotAuthenticated() {
            restClient.get()
                    .uri(ApiConstants.BY_ID, 1L)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        void getAllUsers_returnList_whenUsersExist() {
            final var expectedUserDto = testDataFactory.createUserDto();
            final var expected = List.of(expectedUserDto);

            restClient.get()
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<List<UserDto>>() {
                    }).isEqualTo(expected);
        }

        @Test
        void updateUser_returnOK_whenSuccess() {
            final var updateUserRequest = testDataFactory.createValidUserUpdateRequest();
            final var expected = testDataFactory.createUpdatedUserDto();

            restClient.patch()
                    .uri(ApiConstants.BY_ID, 1L)
                    .headers(TestUtils::authAdmin)
                    .body(updateUserRequest)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(UserDto.class).isEqualTo(expected);
        }

        @Test
        void updateUser_returnBadRequest_whenDtoIsInvalid() {
            final var invalidUpdateRequest = testDataFactory.createInvalidUserUpdateRequest();
            final var expected = Map.of(
                    "message", "Validation for request failed.",
                    "details", Map.of(
                            "firstName", "First name is too short.",
                            "lastName", "Last name is too short.",
                            "email", "Email format is not valid."));

            restClient.patch()
                    .uri(ApiConstants.BY_ID, 1L)
                    .headers(TestUtils::authUser)
                    .body(invalidUpdateRequest)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody(Map.class).isEqualTo(expected);
        }

        @Test
        void deleteUser_returnNoContent_whenSuccess() {
            restClient.delete()
                    .uri(ApiConstants.BY_ID, 1L)
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isNoContent();
        }

        @Test
        void deleteUser_returnUnauthorized_whenNotAuthenticated() {
            restClient.delete()
                    .uri(ApiConstants.BY_ID, 1L)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

    }

    @Nested
    class WithoutRegisteredUser {

        @Test
        void registerUser_success_whenDtoIsValid() {
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
        void registerUser_returnBadRequest_whenDtoIsInvalid() {
            final var invalidDto = new RegisterDto(
                    "Supercalifragilisticexpialidocious",
                    null,
                    "OK",
                    "notValidMail@mail@mail",
                    "lembas");
            final var expectedMap = Map.of("message", "Validation for request failed.",
                    "details", Map.of(
                            "firstName", "First name is too long.",
                            "lastName", "Last name is required.",
                            "userName", "Username is too short, must be longer than 3 characters.",
                            "email", "Email format is not valid.",
                            "password", "Password is too short, must be longer than 7 characters.")
            );

            restClient.post()
                    .body(invalidDto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody(Map.class).isEqualTo(expectedMap);
        }

        @Test
        void getUserById_returnNotFound_whenUserDoesNotExist() {
            final var nonExistentId = 666L;
            final var expectedMessage = "User with id '" + nonExistentId + "' not found.";

            restClient.get()
                    .uri(ApiConstants.BY_ID, nonExistentId)
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(String.class).isEqualTo(expectedMessage);
        }

        @Test
        void getUserById_returnBadRequest_whenIdIsNegative() {
            final var negativeId = -1L;
            final var expectedMessage = "ID must be greater than 0.";

            restClient.get()
                    .uri(ApiConstants.BY_ID, negativeId)
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody(String.class).isEqualTo(expectedMessage);
        }

        @Test
        void getAllUsers_returnEmptyList_whenNoUsersExist() {
            restClient.get()
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(List.class).isEqualTo(Collections.emptyList());
        }

        @Test
        void getAllUsers_returnUnauthorized_whenNotAuthenticated() {
            restClient.get()
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        void updateUser_returnNotFound_whenUserDoesNotExist() {
            final var nonExistentId = 666L;
            final var validDto = testDataFactory.createValidUserUpdateRequest();
            final var expectedMessage = "User with id '" + nonExistentId + "' not found.";

            restClient.patch()
                    .uri(ApiConstants.BY_ID, nonExistentId)
                    .headers(TestUtils::authUser)
                    .body(validDto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(String.class).isEqualTo(expectedMessage);
        }

        @Test
        void updateUser_returnUnauthorized_whenNotAuthenticated() {
            restClient.patch()
                    .uri(ApiConstants.BY_ID, 1L)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        void deleteUser_returnNotFound_whenUserDoesNotExist() {
            final var nonExistentId = 666L;
            final var expectedMessage = "User with id '" + nonExistentId + "' not found.";

            restClient.delete()
                    .uri(ApiConstants.BY_ID, nonExistentId)
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(String.class).isEqualTo(expectedMessage);
        }

    }

}
