package pl.prawko.prawko_server.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pl.prawko.prawko_server.model.RefreshToken;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for {@link RefreshToken} entities.
 * <p>
 * Provides standard CRUD operations through {@link JpaRepository} and custom methods.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Retrieves a refresh token with its user by the token's hash.
     *
     * @param tokenHash SHA-256 hash of the refresh token
     * @return a refresh token when found
     */
    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(final String tokenHash);

    /**
     * Revokes a refresh token unless it's already revoked. Atomic, so of concurrent requests using the same token
     * only one succeeds.
     *
     * @param id the ID of the refresh token
     * @return {@code 1} if the token was revoked by this call, {@code 0} if it already was
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.id = :id AND t.revoked = false")
    int revokeIfActive(final long id);

    /**
     * Revokes all refresh tokens of a user.
     *
     * @param userId the ID of the user owning the tokens
     * @return the number of revoked tokens
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.id = :userId AND t.revoked = false")
    int revokeAllByUserId(final long userId);

    /**
     * Deletes a refresh token by the token's hash.
     *
     * @param tokenHash SHA-256 hash of the refresh token
     * @return {@code 1} if the token was deleted, {@code 0} if it doesn't exist
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.tokenHash = :tokenHash")
    int deleteByTokenHash(final String tokenHash);

    /**
     * Deletes refresh tokens, revoked or not, that expired before the given time.
     *
     * @param expiredBefore the time the tokens must have expired before
     * @return the number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expires < :expiredBefore")
    int deleteExpiredBefore(final Instant expiredBefore);

}
