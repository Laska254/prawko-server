package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.constants.AuthConstants;
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
        final var response = restClient.post()
                .uri(ApiConstants.AUTH_BASE_URL)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(AuthConstants.LOGIN_SUCCESS_MESSAGE);
    }

    @Test
    void login_returnsUnauthorized_whenCredentialsInvalid() {
        final var request = testDataFactory.createInvalidLoginRequest();
        final var response = restClient.post()
                .uri(ApiConstants.AUTH_BASE_URL)
                .body(request)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(String.class), res.getStatusCode()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(AuthConstants.LOGIN_FAILURE_MESSAGE);
    }

}
