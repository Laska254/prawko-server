package pl.prawko.prawko_server.config;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.util.Base64;

/**
 * Configures signed JWT access tokens, issued by {@link pl.prawko.prawko_server.service.IAccessTokenService} and
 * accepted as {@code Authorization: Bearer ...} by {@link SpringSecurity}.
 *
 * <p>Tokens are signed with HS256, as the server is the only party issuing and validating them. The key is the
 * Base64-encoded {@code access-token.secret}, at least 256 bits. Without it a random key is generated at startup, so
 * tokens don't survive a restart - meant for {@code dev} and {@code test}, {@code prod} requires the secret.
 *
 * <p>The claims hold the user ID as subject, the username and the authorities, so requests are authenticated without
 * querying the database. Changes to the account, like its role or deletion, take effect once the token expires.
 */
@Configuration
public class JwtConfig {

    public static final String USERNAME_CLAIM = "username";
    public static final String AUTHORITIES_CLAIM = "authorities";

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
    private static final String KEY_ALGORITHM = "HmacSHA256";
    private static final int GENERATED_KEY_BYTES = 32;

    /**
     * Converts the claims of a validated access token into an authentication with an {@link AuthenticatedUser}, so
     * the principal is the same as after signing in with a password.
     *
     * @param jwt the validated access token
     * @return the authenticated token, without credentials
     */
    public static UsernamePasswordAuthenticationToken toAuthentication(final Jwt jwt) {
        final var principal = new AuthenticatedUser(
                Long.parseLong(jwt.getSubject()),
                jwt.getClaimAsString(USERNAME_CLAIM),
                null,
                true,
                AuthorityUtils.createAuthorityList(jwt.getClaimAsStringList(AUTHORITIES_CLAIM)));
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    @Bean
    public SecretKey accessTokenKey(@Value("${access-token.secret:#{null}}") @Nullable final String secret) {
        if (secret == null) {
            log.info("No access token secret configured, generated a random signing key.");
            return new SecretKeySpec(KeyGenerators.secureRandom(GENERATED_KEY_BYTES).generateKey(), KEY_ALGORITHM);
        }
        return new SecretKeySpec(Base64.getDecoder().decode(secret), KEY_ALGORITHM);
    }

    /**
     * Provides the {@link JwtEncoder} signing access tokens.
     *
     * @param accessTokenKey the signing key
     * @return the HS256 {@link JwtEncoder}
     * @throws IllegalArgumentException if the key is shorter than 256 bits
     */
    @Bean
    public JwtEncoder jwtEncoder(final SecretKey accessTokenKey) {
        return NimbusJwtEncoder.withSecretKey(accessTokenKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Provides the {@link JwtDecoder} validating access tokens: the HS256 signature, the {@code JWT} type and the
     * expiry against the application's {@link Clock}.
     *
     * @param accessTokenKey the signing key
     * @param clock          the clock the expiry is checked against
     * @return the HS256 {@link JwtDecoder}
     */
    @Bean
    public JwtDecoder jwtDecoder(final SecretKey accessTokenKey, final Clock clock) {
        final var timestampValidator = new JwtTimestampValidator();
        timestampValidator.setClock(clock);
        final var decoder = NimbusJwtDecoder.withSecretKey(accessTokenKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(timestampValidator));
        return decoder;
    }

}
