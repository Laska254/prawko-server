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
import java.time.Instant;

/**
 * Implementation of {@link IRefreshTokenService} storing refresh tokens as {@link RefreshToken}s.
 * <p>
 * Tokens come from {@link SecureTokens}, only their hash is stored. Every refresh issues a token valid for the full
 * {@code refresh-token.validity}, so a client used regularly stays signed in.
 * <p>
 * A used token may be used again within {@code refresh-token.reuse-grace} of its first use, for a client whose
 * response was lost, as long as its successor was never used. The retry revokes that successor and issues a new one,
 * so a token has at most one live successor.
 */
@Service
public class RefreshTokenService implements IRefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final String INVALID_TOKEN = "Refresh token is invalid, expired or revoked.";

    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;
    private final IAccessTokenService accessTokenService;
    private final Duration validity;
    private final Duration reuseGrace;
    private final Clock clock;

    public RefreshTokenService(final RefreshTokenRepository repository,
                               final UserRepository userRepository,
                               final IAccessTokenService accessTokenService,
                               @Value("${refresh-token.validity}") final Duration validity,
                               @Value("${refresh-token.reuse-grace}") final Duration reuseGrace,
                               final Clock clock) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.accessTokenService = accessTokenService;
        this.validity = validity;
        this.reuseGrace = reuseGrace;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthTokensDto issueTokens(final AuthenticatedUser user) {
        final var refreshToken = SecureTokens.generate();
        save(refreshToken, user.getId());
        return issuePair(user, refreshToken);
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
        final var userId = token.getUser().getId();
        final var now = clock.instant();
        if (!token.getExpires().isAfter(now)) {
            log.warn("Expired refresh token of user with id '{}' used.", userId);
            throw new InvalidRefreshTokenException(INVALID_TOKEN);
        }
        if (token.getRevokedAt() == null && repository.revokeIfActive(token.getId(), now) == 1) {
            log.info("Refresh token of user with id '{}' used.", userId);
            return replace(token);
        }
        if (isRetry(token, now) && repository.revokeIfActive(token.getReplacedBy().getId(), now) == 1) {
            log.info("Refresh token of user with id '{}' used again within the grace period, revoked its successor.",
                    userId);
            return replace(token);
        }
        final var revoked = repository.revokeAllByUserId(userId, now);
        log.warn("Revoked refresh token of user with id '{}' reused, revoked their {} active refresh tokens.",
                userId, revoked);
        throw new InvalidRefreshTokenException(INVALID_TOKEN);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The token is deleted rather than marked revoked, so its later use isn't taken for reuse of a copied token.
     */
    @Override
    @Transactional
    public void revoke(final String refreshToken) {
        if (repository.deleteByTokenHash(SecureTokens.hash(refreshToken)) == 0) {
            log.info("Unknown refresh token revoked, nothing to do.");
            return;
        }
        log.info("Refresh token revoked.");
    }

    @Override
    @Transactional
    public void revokeAll(final long userId) {
        final var revoked = repository.deleteAllByUserId(userId);
        log.info("Revoked {} refresh tokens of user with id '{}'.", revoked, userId);
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

    private boolean isRetry(final RefreshToken token, final Instant now) {
        return token.getRevokedAt() != null
                && token.getRevokedAt().plus(reuseGrace).isAfter(now)
                && token.getReplacedBy() != null;
    }

    private AuthTokensDto replace(final RefreshToken token) {
        final var refreshToken = SecureTokens.generate();
        repository.setReplacedBy(token.getId(), save(refreshToken, token.getUser().getId()));
        return issuePair(AuthenticatedUser.of(token.getUser()), refreshToken);
    }

    private RefreshToken save(final String refreshToken, final long userId) {
        final var token = new RefreshToken()
                .setTokenHash(SecureTokens.hash(refreshToken))
                .setUser(userRepository.getReferenceById(userId))
                .setExpires(clock.instant().plus(validity));
        repository.save(token);
        log.info("Refresh token issued for user with id '{}'.", userId);
        return token;
    }

    private AuthTokensDto issuePair(final AuthenticatedUser user, final String refreshToken) {
        final var accessToken = accessTokenService.issue(user);
        return new AuthTokensDto(
                accessToken.getTokenValue(),
                refreshToken,
                Duration.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()).toSeconds());
    }

}
