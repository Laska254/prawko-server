package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.LoginDto;

@IntegrationTest
public class AuthControllerTest {

    @LocalServerPort
    private int port;

    private RestTestClient restClient;

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

    @Test
    void login_returnsUnauthorized_whenCredentialsAreInvalid() {
        final var request = new LoginDto("nonExistentUser", "wrongPassword");
        final var expectedMessage = "Bad credentials";

        restClient.post()
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(String.class)
                .isEqualTo(expectedMessage);
    }

    @Test
    void login_returnsBadRequest_whenUsernameIsBlank() {
        final var request = new LoginDto("", "lembasy");

        restClient.post()
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Validation for request failed.")
                .jsonPath("$.details.userName").isEqualTo("Username is required.");
    }

    @Test
    void login_returnsBadRequest_whenPasswordIsBlank() {
        final var request = new LoginDto("pippin", "");

        restClient.post()
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Validation for request failed.")
                .jsonPath("$.details.password").isEqualTo("Password is required.");
    }

    @Test
    void login_returnsBadRequest_whenBodyIsMissing() {
        final var expectedMessage = "Request body is missing.";

        restClient.post()
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .isEqualTo(expectedMessage);
    }

    @Test
    void login_returnsBadRequest_whenBothFieldsAreNull() {
        final var request = new LoginDto(null, null);

        restClient.post()
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Validation for request failed.")
                .jsonPath("$.details.userName").isEqualTo("Username is required.")
                .jsonPath("$.details.password").isEqualTo("Password is required.");
    }

    @Test
    void login_returnsBadRequest_whenLoginIsNull() {
        final var request = new LoginDto(null, "password");

        restClient.post()
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Validation for request failed.")
                .jsonPath("$.details.userName").isEqualTo("Username is required.");
    }

    @Test
    void login_returnsBadRequest_whenPasswordIsNull() {
        final var request = new LoginDto("pippin", null);

        restClient.post()
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Validation for request failed.")
                .jsonPath("$.details.password").isEqualTo("Password is required.");
    }
    
}
