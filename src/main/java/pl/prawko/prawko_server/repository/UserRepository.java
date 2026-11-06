package pl.prawko.prawko_server.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.model.UserIdentity;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for {@link User} entities.
 * <p>
 * Provides standard CRUD operations through {@link JpaRepository} and custom methods.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Checks whether {@link User} already exists with specified username, ignoring case.
     *
     * @param userName provided username to check for existence
     * @return {@code true} if a user already exists, {@code false} otherwise
     */
    boolean existsByUserNameIgnoreCase(final String userName);

    /**
     * Checks whether {@link User} already exists with specified email, ignoring case.
     *
     * @param email provided email to check for existence
     * @return {@code true} if a user already exists, {@code false} otherwise
     */
    boolean existsByEmailIgnoreCase(final String email);

    /**
     * Retrieves {@code user} by its userName or email, both ignoring case.
     *
     * <p>Unambiguous, since usernames can't contain '@' and emails must.
     *
     * @param userName provided name to look for
     * @param email    provided email to look for
     * @return a {@code user} when found
     */
    Optional<User> findByUserNameOrEmailAllIgnoreCase(final String userName, final String email);

    /**
     * Retrieves {@code user} by its email, ignoring case.
     *
     * @param email provided email to look for
     * @return a {@code user} when found
     */
    Optional<User> findByEmailIgnoreCase(final String email);

    /**
     * Retrieves {@code user} linked to the provider account through a {@link UserIdentity}.
     *
     * @param provider       the provider of the account
     * @param providerUserId the provider's ID of the account
     * @return a {@code user} when found
     */
    @Query("SELECT i.user FROM UserIdentity i WHERE i.provider = :provider AND i.providerUserId = :providerUserId")
    Optional<User> findByIdentity(final IdentityProvider provider, final String providerUserId);

    /**
     * Retrieves {@code user} by its ID, locking its row until the end of the transaction.
     *
     * @param id the ID to look for
     * @return a {@code user} when found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findWithLockById(final long id);

    /**
     * Retrieves {@code user} by the hash of its pending password reset token.
     *
     * @param passwordResetTokenHash SHA-256 hash of the password reset token
     * @return a {@code user} when found
     */
    Optional<User> findByPasswordResetTokenHash(final String passwordResetTokenHash);

    /**
     * Retrieves {@code user} by the hash of either of its pending email verification tokens: the one sent to the new
     * (or not yet verified) address or the one sent to the current address to confirm an email change.
     *
     * @param tokenHash SHA-256 hash of the token
     * @return a {@code user} when found
     */
    @Query("SELECT u FROM User u WHERE u.verificationTokenHash = :tokenHash OR u.currentEmailTokenHash = :tokenHash")
    Optional<User> findByVerificationOrCurrentEmailTokenHash(final String tokenHash);

    /**
     * Deletes unverified users whose email verification token expired before the given time.
     *
     * @param expiredBefore the time the tokens must have expired before
     * @return the number of deleted users
     */
    long deleteByEnabledFalseAndVerificationTokenExpiresBefore(final Instant expiredBefore);

}
