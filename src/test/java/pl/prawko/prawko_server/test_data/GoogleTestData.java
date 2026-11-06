package pl.prawko.prawko_server.test_data;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Google ID tokens signed by a test key, whose public part {@link #jwks()} stands in for Google's JWKS.
 */
public class GoogleTestData {

    public static final String ANDROID_CLIENT_ID = "android.apps.googleusercontent.com";
    public static final String WEB_CLIENT_ID = "web.apps.googleusercontent.com";
    public static final String OTHER_CLIENT_ID = "other-client";
    public static final String HOSTED_DOMAIN = "hd";
    public static final String ISSUER = "https://accounts.google.com";
    public static final String INVALID_ID_TOKEN = "Google ID token is invalid or expired.";
    public static final String GOOGLE_NOT_AVAILABLE = "Sign-in with Google is not available.";
    public static final String GOOGLE_UNAVAILABLE = "Sign-in with Google is unavailable, try again later.";
    public static final String EMAIL_NOT_HOSTED = "User with this email already exists, and Google doesn't host the "
            + "email, so the account can't be linked to it. Sign in to that account and link Google from the "
            + "account settings.";
    public static final String ALREADY_LINKED = "User is already linked to a Google account.";
    public static final String LINKED_TO_OTHER_USER = "This Google account is already linked to another user.";
    public static final String NOT_LINKED = "User has no Google account linked.";

    private static final String KEY_ID = "google-test-key";
    private static final RSAKey SIGNING_KEY = generateKey();
    private static final RSAKey OTHER_KEY = generateKey();

    private GoogleTestData() {
    }

    public static JWKSet jwks() {
        return new JWKSet(SIGNING_KEY.toPublicJWK());
    }

    /**
     * Claims of a valid ID token for Pippin's Google Workspace account, with his verified email hosted by Google.
     */
    public static JwtClaimsSet.Builder createPippinClaims() {
        final var pippin = UserTestData.createTestUserPippin();
        final var now = Instant.now();
        return JwtClaimsSet.builder()
                .issuer(ISSUER)
                .audience(List.of(WEB_CLIENT_ID))
                .subject(UserTestData.PROVIDER_USER_ID)
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(1)))
                .claim(StandardClaimNames.EMAIL, pippin.getEmail())
                .claim(StandardClaimNames.EMAIL_VERIFIED, true)
                .claim(HOSTED_DOMAIN, StringUtils.substringAfter(pippin.getEmail(), "@"))
                .claim(StandardClaimNames.GIVEN_NAME, pippin.getFirstName())
                .claim(StandardClaimNames.FAMILY_NAME, pippin.getLastName());
    }

    public static JwtClaimsSet.Builder createPippinClaimsWithoutHostedDomain() {
        return createPippinClaims()
                .claims(claims -> claims.remove(HOSTED_DOMAIN));
    }

    public static String idToken(final JwtClaimsSet.Builder claims) {
        return sign(SIGNING_KEY, claims);
    }

    /**
     * Signs the claims with a key that isn't in the JWKS but has the same key ID.
     */
    public static String idTokenSignedByOtherKey(final JwtClaimsSet.Builder claims) {
        return sign(OTHER_KEY, claims);
    }

    private static String sign(final RSAKey key, final JwtClaimsSet.Builder claims) {
        final var header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(key.getKeyID())
                .type("JWT")
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)))
                .encode(JwtEncoderParameters.from(header, claims.build()))
                .getTokenValue();
    }

    private static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(RSAKeyGenerator.MIN_KEY_SIZE_BITS)
                    .keyID(KEY_ID)
                    .generate();
        } catch (final JOSEException exception) {
            throw new IllegalStateException(exception);
        }
    }

}
