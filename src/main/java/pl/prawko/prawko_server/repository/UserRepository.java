package pl.prawko.prawko_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.prawko.prawko_server.model.User;

import java.util.Optional;

/**
 * Repository for {@link User} entities.
 * <p>
 * Provides standard CRUD operations through {@link JpaRepository} and custom methods.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Checks whether {@link User} already exists with specified username.
     *
     * @param userName provided username to check for existence
     * @return {@code true} if a user already exists, {@code false} otherwise
     */
    boolean existsByUserName(final String userName);

    /**
     * Checks whether {@link User} already exists with specified email, ignoring case.
     *
     * @param email provided email to check for existence
     * @return {@code true} if a user already exists, {@code false} otherwise
     */
    boolean existsByEmailIgnoreCase(final String email);

    /**
     * Retrieves {@code user} by its userName (exact match) or email (ignoring case).
     *
     * <p>Unambiguous, since usernames can't contain '@' and emails must.
     *
     * @param userName provided name to look for
     * @param email    provided email to look for
     * @return An {@code user} when found
     */
    Optional<User> findByUserNameOrEmailIgnoreCase(final String userName, final String email);

}
