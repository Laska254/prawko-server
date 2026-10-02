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

    private static final String USERNAME_SIZE_MSG = "Username or email must not be blank and between 3 and 63 characters.";
    private static final String PASSWORD_SIZE_MSG = "Password must not be blank and between 7 and 63 characters.";

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

}
