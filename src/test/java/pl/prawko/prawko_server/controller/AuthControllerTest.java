package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.LoginDto;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.prawko.prawko_server.config.TestUtils.BASE_URL;

@IntegrationTest
public class AuthControllerTest {

    @LocalServerPort
    private int port;

    private RestClient restClient;

    private <T> ResponseEntity<T> post(final Object body, final Class<T> responseType) {
        return restClient.post()
                .uri(ApiConstants.AUTH_BASE_URL)
                .body(body)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(responseType), res.getStatusCode()));
    }

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl(BASE_URL + port)
                .build();
    }

    @Test
    void login_returnsOk_whenCredentialsAreValid() {
        final var request = new LoginDto("pippin", "lembasy");
        final var expectedMessage = "User signed-in successfully.";

        final var response = post(request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedMessage);
    }

    @Test
    void login_returnsUnauthorized_whenCredentialsAreInvalid() {
        final var request = new LoginDto("nonExistentUser", "wrongPassword");
        final var expectedMessage = "Bad credentials";

        final var response = post(request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(expectedMessage);
    }

    @Test
    void login_returnsBadRequest_whenUsernameIsBlank() {
        final var request = new LoginDto("", "lembasy");

        final var response = post(request, Map.class);
        final var body = (Map<String, Object>) response.getBody();
        final var details = (Map<String, String>) body.get("details");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.get("message")).isEqualTo("Validation for request failed.");
        assertThat(details).containsExactlyEntriesOf(Map.of("userName", "Username is required."));
    }

    @Test
    void login_returnsBadRequest_whenPasswordIsBlank() {
        final var request = new LoginDto("pippin", "");

        final var response = post(request, Map.class);
        final var body = (Map<String, Object>) response.getBody();
        final var details = (Map<String, String>) body.get("details");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.get("message")).isEqualTo("Validation for request failed.");
        assertThat(details).containsExactlyEntriesOf(Map.of("password", "Password is required."));
    }

    @Test
    void login_returnsBadRequest_whenBodyIsMissing() {
        final var expectedMessage = "Request body is missing.";

        final var response = restClient.post()
                .uri(ApiConstants.AUTH_BASE_URL)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(String.class), res.getStatusCode()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(expectedMessage);
    }

}
