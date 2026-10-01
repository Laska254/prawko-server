package pl.prawko.prawko_server.config;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.repository.UserRepository;

/**
 * Authorization rules referenced from method security expressions as {@code @userAuthorization}.
 *
 * <p>Example: {@code @PreAuthorize("hasRole('ADMIN') or @userAuthorization.isSelf(#id, authentication)")}
 */
@Component
public class UserAuthorization {

    private final UserRepository userRepository;

    public UserAuthorization(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Checks whether the authenticated user is the user with the given ID.
     *
     * @param userId         the ID of the user being accessed
     * @param authentication the current {@link Authentication}
     * @return {@code true} if the user with given ID has the authenticated username
     */
    public boolean isSelf(final long userId, final Authentication authentication) {
        return userRepository.existsByIdAndUserName(userId, authentication.getName());
    }

}
