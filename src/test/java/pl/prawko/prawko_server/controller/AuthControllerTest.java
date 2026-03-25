package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.LoginDto;

import java.util.stream.Stream;

@IntegrationTest
public class AuthControllerTest {

    private static final String USERNAME_SIZE_MSG = "Username must not be blank and between 3 and 31 characters.";
    private static final String PASSWORD_SIZE_MSG = "Password must not be blank and between 7 and 63 characters.";
    private static final String USERNAME_REQUIRED_MSG = "Username is required.";
    private static final String PASSWORD_REQUIRED_MSG = "Password is required.";

    @LocalServerPort
    private int port;

    private RestTestClient restClient;

    private static Stream<Arguments> invalidLoginRequests() {
        return Stream.of(
                Arguments.of("both too short", new LoginDto("a".repeat(2), "b".repeat(6)), USERNAME_SIZE_MSG, PASSWORD_SIZE_MSG),
                Arguments.of("both too long", new LoginDto("a".repeat(32), "b".repeat(64)), USERNAME_SIZE_MSG, PASSWORD_SIZE_MSG),
                Arguments.of("both null", new LoginDto(null, null), USERNAME_REQUIRED_MSG, PASSWORD_REQUIRED_MSG),
                Arguments.of("username blank", new LoginDto("", "lembasy"), USERNAME_SIZE_MSG, null),
                Arguments.of("username null", new LoginDto(null, "password"), USERNAME_REQUIRED_MSG, null),
                Arguments.of("password blank", new LoginDto("pippin", "  "), null, PASSWORD_SIZE_MSG),
                Arguments.of("password null", new LoginDto("pippin", null), null, PASSWORD_REQUIRED_MSG)
        );
    }

    @BeforeEach
    void setUp() {
        restClient = TestUtils.createRestTestClient(port, ApiConstants.AUTH_BASE_URL);
    }

    @Test
    void login_returnsOk_whenCredentialsAreValid() {
        final var request = new LoginDto("pippin", "lembasy");
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
    void login_returnsBadRequest_onValidationFailure(
            final String name,
            final LoginDto request,
            final String expectedUserNameError,
            final String expectedPasswordError) {

        final var response = restClient.post()
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Validation for request failed.");

        if (expectedUserNameError != null) {
            response.jsonPath("$.details.userName").isEqualTo(expectedUserNameError);
        }
        if (expectedPasswordError != null) {
            response.jsonPath("$.details.password").isEqualTo(expectedPasswordError);
        }
    }

    @Test
    void login_returnsUnauthorized_whenCredentialsAreInvalid() {
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
    void login_returnsBadRequest_whenBodyIsMissing() {
        final var expectedMessage = "Request body is missing.";

        restClient.post()
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo(expectedMessage);
    }

}
