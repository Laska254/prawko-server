package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.dto.ApiResponse;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.test_data.TestDataFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class UserControllerTest {

    private static final String URL = "/users";

    @LocalServerPort
    private int port;

    private RestClient restClient;

    private final TestDataFactory testDataFactory = new TestDataFactory();

    private void registerUser() {
        restClient.post()
                .uri(URL)
                .body(testDataFactory.createValidRegisterDto())
                .retrieve()
                .toBodilessEntity();
    }

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl(TestUtils.BASE_URL + port)
                .build();
    }

    @Test
    void registerUser_success_whenDtoIsValid() {
        final var expected = "User registered successfully.";
        final var dto = testDataFactory.createValidRegisterDto();

        final var response = restClient.post()
                .uri(URL)
                .body(dto)
                .retrieve()
                .toEntity(ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().message()).isEqualTo(expected);
    }

    @Test
    void registerUser_returnBadRequest_whenDtoIsInvalid() {
        final var dto = testDataFactory.createInvalidRegisterDto();
        final var errorMessage = "Validation for request failed.";
        final var errorDetails = Map.of(
                "firstName", "First name is too long.",
                "lastName", "Last name is required.",
                "userName", "Username must be between 3 and 31 characters.",
                "email", "Email should be valid.",
                "password", "Password must be between 7 and 63 characters."
        );

        final var response = restClient.post()
                .uri(URL)
                .body(dto)
                .exchange((req, res) -> TestUtils.getResponseEntity(res));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo(errorMessage);
        assertThat(response.getBody().details()).isEqualTo(errorDetails);
    }

    @Test
    void registerUser_returnConflict_whenUserAlreadyExists() {
        registerUser();
        final var errorMessage = "User already exists.";
        final var errorDetails = Map.of(
                "email", "User with email 'pippin@shire.me' already exists.",
                "userName", "User with username 'pippin' already exists."
        );

        final var response = restClient.post()
                .uri(URL)
                .body(testDataFactory.createValidRegisterDto())
                .exchange((req, res) -> TestUtils.getResponseEntity(res));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo(errorMessage);
        assertThat(response.getBody().details()).isEqualTo(errorDetails);
    }

    @Test
    void getUserById_returnUserDto_whenFound() {
        registerUser();
        final var response = restClient.get()
                .uri(URL + "/{id}", 1L)
                .headers(TestUtils::authAdmin)
                .retrieve()
                .toEntity(UserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getUserById_returnNotFound_whenUserDoesNotExist() {
        final var nonExistentId = 666L;

        final var response = restClient.get()
                .uri(URL + "/{id}", nonExistentId)
                .headers(TestUtils::authAdmin)
                .exchange((req, res) -> TestUtils.getResponseEntity(res));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("User with id '" + nonExistentId + "' not found.");
    }

    @Test
    void updateUser_returnOK_whenSuccess() {
        registerUser();
        final var updateUserRequest = testDataFactory.createValidUserUpdateRequest();
        final var expectedDetails = Map.of(
                "id", 1,
                "firstName", "UpdatedFirstName",
                "lastName", "UpdatedLastName",
                "userName", "UpdatedUserName",
                "email", "UpdatedEmail@shire.me"
        );

        final var response = restClient.patch()
                .uri(URL + "/{id}", 1L)
                .headers(TestUtils::authUser)
                .body(updateUserRequest)
                .exchange((req, res) -> TestUtils.getResponseEntity(res));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().message()).isEqualTo("User updated successfully.");
        assertThat(response.getBody().details()).isEqualTo(expectedDetails);
    }

    @Test
    void deleteUser_returnNoContent_whenSuccess() {
        registerUser();
        final var response = restClient.delete()
                .uri(URL + "/{id}", 1L)
                .headers(TestUtils::authAdmin)
                .exchange((req, res) -> TestUtils.getResponseEntity(res));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

}
