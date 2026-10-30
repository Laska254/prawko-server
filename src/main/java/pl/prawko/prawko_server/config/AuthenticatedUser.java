package pl.prawko.prawko_server.config;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import pl.prawko.prawko_server.model.Role;

import java.io.Serial;
import java.util.Collection;
import java.util.Optional;

/**
 * Authenticated principal carrying the ID of the corresponding {@link pl.prawko.prawko_server.model.User}.
 *
 * <p>Lets ownership checks compare IDs directly against the principal, without querying the database.
 * Used by {@link IsSelfOrAdmin} and other method security expressions as {@code principal.isSelf(...)},
 * and injectable into controllers via {@link org.springframework.security.core.annotation.AuthenticationPrincipal}.
 *
 * <p>Disabled until the user verifies their email address, which blocks authentication.
 *
 * <p>Has no password when authenticated with an access token, see {@link JwtConfig#toAuthentication}.
 */
public class AuthenticatedUser extends User {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long id;

    public AuthenticatedUser(final long id,
                             final String username,
                             @Nullable final String password,
                             final boolean enabled,
                             final Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, true, true, true, authorities);
        this.id = id;
    }

    /**
     * Creates the principal of a persisted user, with its current password hash, enabled status and role.
     *
     * @param user the persisted user
     * @return the principal of the user
     */
    public static AuthenticatedUser of(final pl.prawko.prawko_server.model.User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getUserName(),
                user.getPassword(),
                user.isEnabled(),
                AuthorityUtils.createAuthorityList(user.getRole().getAuthority()));
    }

    public static Optional<Long> idOf(@Nullable final Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getPrincipal)
                .filter(AuthenticatedUser.class::isInstance)
                .map(AuthenticatedUser.class::cast)
                .map(AuthenticatedUser::getId);
    }

    public long getId() {
        return id;
    }

    public boolean isSelf(final long userId) {
        return id == userId;
    }

    public boolean isAdmin() {
        return getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(Role.ADMIN.getAuthority()::equals);
    }

}
