package pl.prawko.prawko_server.service.implementation;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import pl.prawko.prawko_server.exception.InvalidProviderCredentialException;
import pl.prawko.prawko_server.exception.ProviderNotConfiguredException;
import pl.prawko.prawko_server.exception.ProviderUnavailableException;
import pl.prawko.prawko_server.model.EmailStatus;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.model.ProviderProfile;
import pl.prawko.prawko_server.service.IGoogleProfileService;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link IGoogleProfileService} verifying Google ID tokens.
 * <p>
 * A token is accepted if it's signed with a key from Google's JWKS ({@code google.jwk-set-uri}), not expired, issued
 * by Google and for one of {@code google.client-ids} as audience. Every client, Android included, requests the token
 * for the web (server) client ID, which then is the audience; several can be configured, e.g. one per Google Cloud
 * project. Without any, Google sign-in is disabled; the JWKS is only fetched on the first sign-in, so startup doesn't
 * depend on it either way.
 * <p>
 * Google is authoritative for an email only if it hosts it: a Gmail address, or a Workspace one (with the {@code hd}
 * claim). Any other email was only verified when it was added to the Google account and may have changed hands since.
 */
@Service
public class GoogleProfileService implements IGoogleProfileService {

    private static final Logger log = LoggerFactory.getLogger(GoogleProfileService.class);
    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final String INVALID_TOKEN = "Google ID token is invalid or expired.";
    private static final String HOSTED_DOMAIN = "hd";
    private static final String GMAIL_SUFFIX = "@gmail.com";

    private final @Nullable JwtDecoder decoder;

    public GoogleProfileService(@Value("${google.client-ids}") final List<String> clientIds,
                                @Value("${google.jwk-set-uri}") final String jwkSetUri,
                                final Clock clock) {
        if (clientIds.isEmpty()) {
            this.decoder = null;
            log.info("No Google client IDs configured, sign-in with Google is disabled.");
        } else {
            this.decoder = createDecoder(Set.copyOf(clientIds), jwkSetUri, clock);
        }
    }

    @Override
    public ProviderProfile fetchProfile(final String idToken) {
        if (decoder == null) {
            throw new ProviderNotConfiguredException("Sign-in with Google is not available.");
        }
        try {
            return toProfile(decoder.decode(idToken));
        } catch (final BadJwtException exception) {
            log.warn("Google ID token rejected: {}", exception.getMessage());
            throw new InvalidProviderCredentialException(INVALID_TOKEN);
        } catch (final JwtException exception) {
            log.error("Google ID token couldn't be verified.", exception);
            throw new ProviderUnavailableException("Sign-in with Google is unavailable, try again later.", exception);
        }
    }

    private static JwtDecoder createDecoder(final Set<String> clientIds, final String jwkSetUri, final Clock clock) {
        final var timestampValidator = new JwtTimestampValidator();
        timestampValidator.setClock(clock);
        timestampValidator.setAllowEmptyExpiryClaim(false);
        final var decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
                timestampValidator,
                new JwtClaimValidator<String>(JwtClaimNames.ISS, ISSUERS::contains),
                new JwtClaimValidator<Collection<String>>(JwtClaimNames.AUD,
                        audience -> audience.stream()
                                .anyMatch(clientIds::contains)),
                new JwtClaimValidator<String>(JwtClaimNames.SUB, StringUtils::isNotBlank)));
        return decoder;
    }

    /**
     * Maps the token's claims, suggesting the email's local part as username.
     */
    private static ProviderProfile toProfile(final Jwt jwt) {
        final var email = jwt.getClaimAsString(StandardClaimNames.EMAIL);
        return new ProviderProfile(
                IdentityProvider.GOOGLE,
                jwt.getSubject(),
                email,
                emailStatus(jwt, email),
                StringUtils.substringBefore(email, "@"),
                jwt.getClaimAsString(StandardClaimNames.GIVEN_NAME),
                jwt.getClaimAsString(StandardClaimNames.FAMILY_NAME));
    }

    private static EmailStatus emailStatus(final Jwt jwt, @Nullable final String email) {
        if (email == null || !Boolean.TRUE.equals(jwt.getClaimAsBoolean(StandardClaimNames.EMAIL_VERIFIED))) {
            return EmailStatus.UNVERIFIED;
        }
        if (StringUtils.endsWithIgnoreCase(email, GMAIL_SUFFIX)
                || StringUtils.isNotBlank(jwt.getClaimAsString(HOSTED_DOMAIN))) {
            return EmailStatus.AUTHORITATIVE;
        }
        return EmailStatus.VERIFIED;
    }

}
