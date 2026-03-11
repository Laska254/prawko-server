package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.test_data.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.prawko.prawko_server.config.TestUtils.BASE_URL;

@IntegrationTest
public class AuthControllerTest {

    @LocalServerPort
    private int port;

    private RestClient restClient;

    private final TestDataFactory testDataFactory = new TestDataFactory();

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl(BASE_URL + port)
                .build();
    }

    @Test
    void login_returnsOk_whenCredentialsValid() {
        final var request = testDataFactory.createValidLoginRequest();
        final var message = "User signed-in successfully.";
        final var response = restClient.post()
                .uri(ApiConstants.AUTH_BASE_URL)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(message);
    }

    @Test
    void login_returnsUnauthorized_whenCredentialsInvalid() {
        final var request = testDataFactory.createInvalidLoginRequest();
        final var response = restClient.post()
                .uri(ApiConstants.AUTH_BASE_URL)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                })
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
