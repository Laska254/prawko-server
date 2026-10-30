package pl.prawko.prawko_server.service.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import pl.prawko.prawko_server.config.AuthenticatedUser;
import pl.prawko.prawko_server.config.JwtConfig;
import pl.prawko.prawko_server.service.IAccessTokenService;

import java.time.Clock;
import java.time.Duration;

/**
 * Implementation of {@link IAccessTokenService} signing JWTs with the {@link JwtEncoder} from {@link JwtConfig}.
 */
@Service
public class AccessTokenService implements IAccessTokenService {

    private static final Logger log = LoggerFactory.getLogger(AccessTokenService.class);

    private final JwtEncoder encoder;
    private final Duration validity;
    private final Clock clock;

    public AccessTokenService(final JwtEncoder encoder,
                              @Value("${access-token.validity}") final Duration validity,
                              final Clock clock) {
        this.encoder = encoder;
        this.validity = validity;
        this.clock = clock;
    }

    @Override
    public Jwt issue(final AuthenticatedUser user) {
        final var issuedAt = clock.instant();
        final var claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.getId()))
                .claim(JwtConfig.USERNAME_CLAIM, user.getUsername())
                .claim(JwtConfig.AUTHORITIES_CLAIM, user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(validity))
                .build();
        final var token = encoder.encode(JwtEncoderParameters.from(claims));
        log.info("Access token issued for user with id '{}'.", user.getId());
        return token;
    }

}
