package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.model.Role;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class SpringSecurityTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

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

    @Test
    void corsConfigurationSource_allowPreflightWithoutCredentials_whenOriginIsAllowed() {
        restClient.options()
                .uri(ApiConstants.USERS_BASE_URL)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.AUTHORIZATION)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN);
    }

    @Test
    void corsConfigurationSource_rejectPreflight_whenOriginIsNotAllowed() {
        restClient.options()
                .uri(ApiConstants.USERS_BASE_URL)
                .header(HttpHeaders.ORIGIN, "https://example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    @Test
    void corsConfigurationSource_exposeLocationHeader_whenOriginIsAllowed() {
        restClient.get()
                .uri(ApiConstants.USERS_BASE_URL)
                .headers(TestUtils::authAdmin)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN)
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.LOCATION);
    }

    @Test
    void roleHierarchy_grantUserAuthority_whenUserIsAdmin() {
        final var adminAuthorities = AuthorityUtils.createAuthorityList(Role.ADMIN.getAuthority());

        final var reachable = SpringSecurity.roleHierarchy().getReachableGrantedAuthorities(adminAuthorities);

        assertThat(reachable)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(Role.ADMIN.getAuthority(), Role.USER.getAuthority());
    }

}
