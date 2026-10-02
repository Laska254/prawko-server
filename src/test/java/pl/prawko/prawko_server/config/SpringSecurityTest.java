package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.model.Role;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void roleHierarchy_grantUserAuthority_whenUserIsAdmin() {
        final var adminAuthorities = AuthorityUtils.createAuthorityList(Role.ADMIN.getAuthority());

        final var reachable = SpringSecurity.roleHierarchy().getReachableGrantedAuthorities(adminAuthorities);

        assertThat(reachable)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(Role.ADMIN.getAuthority(), Role.USER.getAuthority());
    }

}
