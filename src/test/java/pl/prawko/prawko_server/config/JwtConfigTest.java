package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    private static final long USER_ID = 7L;

    private final JwtConfig config = new JwtConfig();

    @Test
    void accessTokenKey_decodeSecret_whenSecretIsConfigured() {
        final var secret = new byte[32];
        secret[0] = 1;

        final var key = config.accessTokenKey(Base64.getEncoder().encodeToString(secret));

        assertThat(key.getEncoded()).isEqualTo(secret);
    }

    @Test
    void accessTokenKey_generateRandom256BitKey_whenSecretIsMissing() {
        final var key = config.accessTokenKey(null);

        assertThat(key.getEncoded())
                .hasSize(32)
                .isNotEqualTo(config.accessTokenKey(null).getEncoded());
    }

    @Test
    void jwtEncoder_throwIllegalArgument_whenKeyIsShorterThan256Bits() {
        final var key = new SecretKeySpec(new byte[31], "HmacSHA256");

        assertThatThrownBy(() -> config.jwtEncoder(key))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void jwtDecoder_throwJwtValidation_whenTokenExpiredByClock() {
        final var key = config.accessTokenKey(null);
        final var now = TestUtils.CLOCK.instant();
        final var claims = JwtClaimsSet.builder()
                .subject(String.valueOf(USER_ID))
                .issuedAt(now.minus(Duration.ofMinutes(20)))
                .expiresAt(now.minus(Duration.ofMinutes(5)))
                .build();
        final var token = config.jwtEncoder(key).encode(JwtEncoderParameters.from(claims)).getTokenValue();

        assertThatThrownBy(() -> config.jwtDecoder(key, TestUtils.CLOCK).decode(token))
                .isInstanceOf(JwtValidationException.class);
    }

}
