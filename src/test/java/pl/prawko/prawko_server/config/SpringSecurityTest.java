package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.IAccessTokenService;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class SpringSecurityTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String INVALID_TOKEN = "invalid_token";

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private IAccessTokenService accessTokenService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private UserRepository userRepository;

    @LocalServerPort
    private int port;

    private RestTestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = TestUtils.createRestTestClient(port, "");
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
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
    void filterChain_returnUnauthorized_whenEmailIsNotVerified() {
        restClient.get()
                .uri(ApiConstants.USERS_BASE_URL + ApiConstants.ME)
                .headers(TestUtils::authUnverified)
                .exchange()
                .expectStatus().isUnauthorized();
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
    void filterChain_authenticateTokenOwner_whenBearerTokenIsValid() {
        final var tester = userRepository.save(UserTestData.createTestUserPippin());

        restClient.get()
                .uri(ApiConstants.USERS_BASE_URL + ApiConstants.ME)
                .headers(headers -> headers.setBearerAuth(issueToken(TestUtils.USER_NAME)))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.id").isEqualTo(tester.getId());
    }

    @Test
    void filterChain_grantAdminEndpoint_whenBearerTokenHasAdminRole() {
        restClient.get()
                .uri(ApiConstants.USERS_BASE_URL)
                .headers(headers -> headers.setBearerAuth(issueToken(TestUtils.ADMIN_NAME)))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void filterChain_denyAdminEndpoint_whenBearerTokenHasUserRole() {
        restClient.get()
                .uri(ApiConstants.USERS_BASE_URL)
                .headers(headers -> headers.setBearerAuth(issueToken(TestUtils.USER_NAME)))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void filterChain_returnUnauthorized_whenBearerTokenIsExpired() {
        final var claims = TestUtils.adminClaims(Instant.now().minus(Duration.ofMinutes(5)));
        final var token = jwtEncoder.encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        expectInvalidToken(token);
    }

    @Test
    void filterChain_returnUnauthorized_whenBearerTokenIsMalformed() {
        expectInvalidToken("not-a-jwt");
    }

    @Test
    void filterChain_returnUnauthorized_whenBearerTokenIsSignedWithAnotherKey() {
        final var config = new JwtConfig();
        final var otherEncoder = config.jwtEncoder(config.accessTokenKey(null));
        final var claims = TestUtils.adminClaims(Instant.now().plus(Duration.ofMinutes(5)));
        final var token = otherEncoder.encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        expectInvalidToken(token);
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
    void corsConfigurationSource_exposeLocationAndDateHeaders_whenOriginIsAllowed() {
        restClient.get()
                .uri(ApiConstants.USERS_BASE_URL)
                .headers(TestUtils::authAdmin)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN)
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        String.join(", ", HttpHeaders.LOCATION, HttpHeaders.DATE));
    }

    @Test
    void roleHierarchy_grantUserAuthority_whenUserIsAdmin() {
        final var adminAuthorities = AuthorityUtils.createAuthorityList(Role.ADMIN.getAuthority());

        final var reachable = SpringSecurity.roleHierarchy().getReachableGrantedAuthorities(adminAuthorities);

        assertThat(reachable)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(Role.ADMIN.getAuthority(), Role.USER.getAuthority());
    }

    private String issueToken(final String username) {
        return TestUtils.issueAccessToken(accessTokenService, userDetailsService, username);
    }

    private void expectInvalidToken(final String token) {
        restClient.get()
                .uri(ApiConstants.USERS_BASE_URL)
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().value(HttpHeaders.WWW_AUTHENTICATE, header -> assertThat(header).contains(INVALID_TOKEN));
    }

}
