package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import pl.prawko.prawko_server.model.Role;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    private final JwtConfig config = new JwtConfig();
    private final SecretKey key = config.accessTokenKey(null);

    @Test
    void toAuthentication_returnAuthenticatedUser_whenTokenIsValid() {
        final var claims = TestUtils.adminClaims(TestUtils.CLOCK.instant().plus(Duration.ofMinutes(5)));
        final var jwt = config.jwtEncoder(key)
                .encode(JwtEncoderParameters.from(claims));

        final var authentication = JwtConfig.toAuthentication(jwt);

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getPrincipal())
                .isInstanceOfSatisfying(AuthenticatedUser.class, principal -> {
                    assertThat(principal.getId()).isEqualTo(TestUtils.PRINCIPAL_ID);
                    assertThat(principal.getUsername()).isEqualTo(TestUtils.ADMIN_NAME);
                    assertThat(principal.getPassword()).isNull();
                    assertThat(principal.isEnabled()).isTrue();
                    assertThat(principal.isAdmin()).isTrue();
                });
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.ADMIN.getAuthority());
    }

    @Test
    void accessTokenKey_decodeSecret_whenSecretIsConfigured() {
        final var secret = new byte[32];
        secret[0] = 1;

        final var configuredKey = config.accessTokenKey(Base64.getEncoder().encodeToString(secret));

        assertThat(configuredKey.getEncoded()).isEqualTo(secret);
    }

    @Test
    void accessTokenKey_generateRandom256BitKey_whenSecretIsMissing() {
        assertThat(key.getEncoded())
                .hasSize(32)
                .isNotEqualTo(config.accessTokenKey(null).getEncoded());
    }

    @Test
    void jwtEncoder_throwIllegalArgument_whenKeyIsShorterThan256Bits() {
        final var shortKey = new SecretKeySpec(new byte[31], "HmacSHA256");

        assertThatThrownBy(() -> config.jwtEncoder(shortKey))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void jwtDecoder_throwJwtValidation_whenTokenExpiredByClock() {
        final var claims = TestUtils.adminClaims(TestUtils.CLOCK.instant().minus(Duration.ofMinutes(5)));
        final var token = config.jwtEncoder(key)
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
        final var decoder = config.jwtDecoder(key, TestUtils.CLOCK);

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(JwtValidationException.class);
    }

}
