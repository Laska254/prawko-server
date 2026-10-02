package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.constants.ApiConstants;

@IntegrationTest
class SpringSecurityTest {

    @LocalServerPort
    private int port;

    private RestTestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = TestUtils.createRestTestClient(port, "");
    }

    @Test
    void filterChain_doNotCreateSession_whenRequestIsAuthenticated() {
        restClient.get()
                .uri(ApiConstants.USERS_BASE_URL)
                .headers(TestUtils::authAdmin)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(HttpHeaders.SET_COOKIE);
    }

    @Test
    void filterChain_doNotCreateSession_whenRequestIsUnauthenticated() {
        restClient.get()
                .uri(ApiConstants.USERS_BASE_URL)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().doesNotExist(HttpHeaders.SET_COOKIE);
    }

    @Test
    void filterChain_denyRequest_whenNoAuthorizationRuleMatches() {
        restClient.get()
                .uri("/unmapped")
                .headers(TestUtils::authAdmin)
                .exchange()
                .expectStatus().isForbidden();
    }

}
