package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.Test;
import pl.prawko.prawko_server.config.AuthenticatedUser;
import pl.prawko.prawko_server.config.JwtConfig;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.service.implementation.AccessTokenService;

import javax.crypto.SecretKey;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenServiceTest {

    private static final Duration VALIDITY = Duration.ofMinutes(15);

    private final JwtConfig config = new JwtConfig();
    private final SecretKey key = config.accessTokenKey(null);
    private final AccessTokenService service = new AccessTokenService(config.jwtEncoder(key), VALIDITY, TestUtils.CLOCK);
    private final AuthenticatedUser user = TestUtils.createUserPrincipal();

    @Test
    void issue_putUserIdUsernameAndAuthoritiesIntoClaims_whenUserIsAuthenticated() {
        final var token = service.issue(user);

        assertThat(token.getSubject()).isEqualTo(String.valueOf(user.getId()));
        assertThat(token.getClaimAsString(JwtConfig.USERNAME_CLAIM)).isEqualTo(user.getUsername());
        assertThat(token.getClaimAsStringList(JwtConfig.AUTHORITIES_CLAIM)).containsExactly(Role.USER.getAuthority());
    }

    @Test
    void issue_expireAfterValidity_whenUserIsAuthenticated() {
        final var token = service.issue(user);

        assertThat(token.getIssuedAt()).isEqualTo(TestUtils.CLOCK.instant());
        assertThat(token.getExpiresAt()).isEqualTo(TestUtils.CLOCK.instant().plus(VALIDITY));
    }

    @Test
    void issue_returnTokenAcceptedByDecoder_whenUserIsAuthenticated() {
        final var token = service.issue(user);

        final var decoded = config.jwtDecoder(key, TestUtils.CLOCK)
                .decode(token.getTokenValue());
        assertThat(decoded.getSubject()).isEqualTo(token.getSubject());
    }

}
