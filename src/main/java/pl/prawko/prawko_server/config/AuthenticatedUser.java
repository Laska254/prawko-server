package pl.prawko.prawko_server.config;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
 */
public class AuthenticatedUser extends User {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long id;

    public AuthenticatedUser(final long id,
                             final String username,
                             final String password,
                             final Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
    }

    public static Optional<Long> idOf(@Nullable final Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user
                ? Optional.of(user.getId())
                : Optional.empty();
    }

    public long getId() {
        return id;
    }
    
    public boolean isSelf(final long userId) {
        return id == userId;
    }

    public boolean isAdmin() {
        return getAuthorities().stream()
                .anyMatch(authority -> Role.ADMIN.getAuthority().equals(authority.getAuthority()));
    }

}
