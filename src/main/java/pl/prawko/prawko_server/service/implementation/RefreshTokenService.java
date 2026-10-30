package pl.prawko.prawko_server.service.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.prawko.prawko_server.config.AuthenticatedUser;
import pl.prawko.prawko_server.dto.AuthTokensDto;
import pl.prawko.prawko_server.exception.InvalidRefreshTokenException;
import pl.prawko.prawko_server.model.RefreshToken;
import pl.prawko.prawko_server.repository.RefreshTokenRepository;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.IAccessTokenService;
import pl.prawko.prawko_server.service.IRefreshTokenService;
import pl.prawko.prawko_server.util.SecureTokens;

import java.time.Clock;
import java.time.Duration;

/**
 * Implementation of {@link IRefreshTokenService} storing refresh tokens as {@link RefreshToken}s.
 * <p>
 * Tokens come from {@link SecureTokens}, only their hash is stored. Every refresh issues a token valid for the full
 * {@code refresh-token.validity}, so a client used regularly stays signed in.
 */
@Service
public class RefreshTokenService implements IRefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final String INVALID_TOKEN = "Refresh token is invalid, expired or revoked.";

    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;
    private final IAccessTokenService accessTokenService;
    private final Duration validity;
    private final Clock clock;

    public RefreshTokenService(final RefreshTokenRepository repository,
                               final UserRepository userRepository,
                               final IAccessTokenService accessTokenService,
                               @Value("${refresh-token.validity}") final Duration validity,
                               final Clock clock) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.accessTokenService = accessTokenService;
        this.validity = validity;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthTokensDto issueTokens(final AuthenticatedUser user) {
        final var refreshToken = SecureTokens.generate();
        repository.save(new RefreshToken()
                .setTokenHash(SecureTokens.hash(refreshToken))
                .setUser(userRepository.getReferenceById(user.getId()))
                .setExpires(clock.instant().plus(validity)));
        log.info("Refresh token issued for user with id '{}'.", user.getId());
        final var accessToken = accessTokenService.issue(user);
        return new AuthTokensDto(
                accessToken.getTokenValue(),
                refreshToken,
                Duration.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()).toSeconds());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Revoking all tokens of the user on reuse is committed even though the refresh fails.
     */
    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthTokensDto refresh(final String refreshToken) {
        final var token = repository.findByTokenHash(SecureTokens.hash(refreshToken))
                .orElseThrow(() -> {
                    log.warn("Unknown refresh token used.");
                    return new InvalidRefreshTokenException(INVALID_TOKEN);
                });
        final var user = token.getUser();
        if (!token.getExpires().isAfter(clock.instant())) {
            log.warn("Expired refresh token of user with id '{}' used.", user.getId());
            throw new InvalidRefreshTokenException(INVALID_TOKEN);
        }
        if (token.isRevoked() || repository.revokeIfActive(token.getId()) == 0) {
            final var revoked = repository.revokeAllByUserId(user.getId());
            log.warn("Revoked refresh token of user with id '{}' reused, revoked their {} active refresh tokens.",
                    user.getId(), revoked);
            throw new InvalidRefreshTokenException(INVALID_TOKEN);
        }
        log.info("Refresh token of user with id '{}' used.", user.getId());
        return issueTokens(AuthenticatedUser.of(user));
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${refresh-token.cleanup-interval}",
            initialDelayString = "${refresh-token.cleanup-interval}")
    public void deleteExpiredTokens() {
        final var deleted = repository.deleteExpiredBefore(clock.instant());
        if (deleted > 0) {
            log.info("Deleted {} expired refresh tokens.", deleted);
        }
    }

}
