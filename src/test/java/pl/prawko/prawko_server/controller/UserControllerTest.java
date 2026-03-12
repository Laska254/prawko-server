package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.test_data.TestDataFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class UserControllerTest {

    @Autowired
    private UserRepository userRepository;

    @LocalServerPort
    private int port;

    private RestClient restClient;

    private final TestDataFactory testDataFactory = new TestDataFactory();
    private final RegisterDto registerDto = testDataFactory.createValidRegisterDto();

    private ResponseEntity<Void> registerUser() {
        return restClient.post()
                .uri(ApiConstants.USERS_BASE_URL)
                .body(registerDto)
                .retrieve()
                .toBodilessEntity();
    }

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl(TestUtils.BASE_URL + port)
                .build();
    }

    //----------------------------------------------- POST -> /users -----------------------------------------------

    @Test
    void registerUser_success_whenDtoIsValid() {
        final var response = registerUser();

        final var createdUser = userRepository.findAll().getFirst();
        final var expectedLocation = ApiConstants.USERS_BASE_URL + "/" + createdUser.getId();
        final var location = response.getHeaders().getLocation().getPath();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(location).isEqualTo(expectedLocation);
    }

    @Test
    void registerUser_returnBadRequest_whenDtoIsInvalid() {
        final var dto = testDataFactory.createInvalidRegisterDto();
        final var errorMessage = "Validation for request failed.";
        final var errorDetails = Map.of(
                "firstName", "First name is too long.",
                "lastName", "Last name is required.",
                "userName", "Username is too short, must be longer than 3 characters.",
                "email", "Email format is not valid.",
                "password", "Password is too short, must be longer than 7 characters."
        );

        final var response = restClient.post()
                .uri(ApiConstants.USERS_BASE_URL)
                .body(dto)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(Map.class), res.getStatusCode()));
        final var body = (Map<String, Object>) response.getBody();
        final var message = body.get("message");
        final var details = (Map<String, String>) body.get("details");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(message).isEqualTo(errorMessage);
        assertThat(details).containsAllEntriesOf(errorDetails);
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
                .uri(ApiConstants.USERS_BASE_URL)
                .body(registerDto)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(Map.class), res.getStatusCode()));
        final var body = (Map<String, Object>) response.getBody();
        final var message = body.get("message");
        final var details = (Map<String, String>) body.get("details");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(message).isEqualTo(errorMessage);
        assertThat(details).containsAllEntriesOf(errorDetails);
    }

    //--------------------------------------------- GET -> /users/{id} ---------------------------------------------

    @Test
    void getUserById_returnUserDto_whenFound() {
        registerUser();
        final var expectedUserDto = testDataFactory.createUserDto();

        final var response = restClient.get()
                .uri(ApiConstants.USERS_BASE_URL + ApiConstants.BY_ID, 1L)
                .headers(TestUtils::authAdmin)
                .retrieve()
                .toEntity(UserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedUserDto);
    }

    @Test
    void getUserById_returnNotFound_whenUserDoesNotExist() {
        final var nonExistentId = 666L;
        final var expectedMessage = "User with id '" + nonExistentId + "' not found.";

        final var response = restClient.get()
                .uri(ApiConstants.USERS_BASE_URL + ApiConstants.BY_ID, nonExistentId)
                .headers(TestUtils::authAdmin)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(String.class), res.getStatusCode()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(expectedMessage);
    }

    @Test
    void getUserById_returnBadRequest_whenIdIsNegative() {
        final var negativeId = -1L;
        final var expectedMessage = "getUserById.id: must be greater than 0";
        final var response = restClient.get()
                .uri(ApiConstants.USERS_BASE_URL + ApiConstants.BY_ID, negativeId)
                .headers(TestUtils::authAdmin)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(String.class), res.getStatusCode()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(expectedMessage);
    }

    @Test
    void getUserById_returnUnauthorized_whenNotAuthenticated() {
        registerUser();

        final var response = restClient.get()
                .uri(ApiConstants.USERS_BASE_URL + ApiConstants.BY_ID, 1L)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(String.class), res.getStatusCode()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    //------------------------------------------------ GET -> /users -----------------------------------------------

    @Test
    void getAllUsers_returnList_whenUsersExist() {
        registerUser();
        final var expectedUserDto = testDataFactory.createUserDto();

        final var response = restClient.get()
                .uri(ApiConstants.USERS_BASE_URL)
                .headers(TestUtils::authAdmin)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<List<UserDto>>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsOnly(expectedUserDto);
    }

    @Test
    void getAllUsers_returnEmptyList_whenNoUsersExist() {
        final var response = restClient.get()
                .uri(ApiConstants.USERS_BASE_URL)
                .headers(TestUtils::authAdmin)
                .retrieve()
                .toEntity(List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getAllUsers_returnUnauthorized_whenNotAuthenticated() {
        final var response = restClient.get()
                .uri(ApiConstants.USERS_BASE_URL)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(String.class), res.getStatusCode()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    //--------------------------------------------- PATCH -> /users/{id} -------------------------------------------

    @Test
    void updateUser_returnOK_whenSuccess() {
        registerUser();
        final var updateUserRequest = testDataFactory.createValidUserUpdateRequest();
        final var expected = testDataFactory.createUpdatedUserDto();

        final var response = restClient.patch()
                .uri(ApiConstants.USERS_BASE_URL + ApiConstants.BY_ID, 1L)
                .headers(TestUtils::authUser)
                .body(updateUserRequest)
                .retrieve()
                .toEntity(UserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void updateUser_returnBadRequest_whenDtoIsInvalid() {
        registerUser();
        final var invalidUpdateRequest = testDataFactory.createInvalidUserUpdateRequest();
        final var errorMessage = "Validation for request failed.";
        final var errorDetails = Map.of(
                "firstName", "First name is too short.",
                "lastName", "Last name is too short.",
                "email", "Email format is not valid."
        );

        final var response = restClient.patch()
                .uri(ApiConstants.USERS_BASE_URL + ApiConstants.BY_ID, 1L)
                .headers(TestUtils::authUser)
                .body(invalidUpdateRequest)
                .exchange((req, res) ->
                        new ResponseEntity<>(
                                res.bodyTo(new ParameterizedTypeReference<Map<String, Object>>() {
                                }),
                                res.getStatusCode()
                        ));
        final var body = response.getBody();
        final var message = body.get("message");
        final var details = (Map<String, String>) body.get("details");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(message).isEqualTo(errorMessage);
        assertThat(details).containsAllEntriesOf(errorDetails);
    }

    @Test
    void updateUser_returnNotFound_whenUserDoesNotExist() {
        final var nonExistentId = 666L;
        final var updateUserRequest = testDataFactory.createValidUserUpdateRequest();
        final var expectedMessage = "User with id '" + nonExistentId + "' not found.";

        final var response = restClient.patch()
                .uri(ApiConstants.USERS_BASE_URL + ApiConstants.BY_ID, nonExistentId)
                .headers(TestUtils::authUser)
                .body(updateUserRequest)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(String.class), res.getStatusCode()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(expectedMessage);
    }

    @Test
    void updateUser_returnUnauthorized_whenNotAuthenticated() {
        registerUser();
        final var updateUserRequest = testDataFactory.createValidUserUpdateRequest();

        final var response = restClient.patch()
                .uri(ApiConstants.USERS_BASE_URL + ApiConstants.BY_ID, 1L)
                .body(updateUserRequest)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(String.class), res.getStatusCode()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    //-------------------------------------------- DELETE -> /users/{id} -------------------------------------------

    @Test
    void deleteUser_returnNoContent_whenSuccess() {
        registerUser();

        final var response = restClient.delete()
                .uri(ApiConstants.USERS_BASE_URL + ApiConstants.BY_ID, 1L)
                .headers(TestUtils::authAdmin)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteUser_returnNotFound_whenUserDoesNotExist() {
        final var nonExistentId = 666L;
        final var expectedMessage = "User with id '" + nonExistentId + "' not found.";

        final var response = restClient.delete()
                .uri(ApiConstants.USERS_BASE_URL + ApiConstants.BY_ID, nonExistentId)
                .headers(TestUtils::authAdmin)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(String.class), res.getStatusCode()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(expectedMessage);
    }

    @Test
    void deleteUser_returnUnauthorized_whenNotAuthenticated() {
        registerUser();

        final var response = restClient.delete()
                .uri(ApiConstants.USERS_BASE_URL + ApiConstants.BY_ID, 1L)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(String.class), res.getStatusCode()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

}
