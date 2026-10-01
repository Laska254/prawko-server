package pl.prawko.prawko_server.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import pl.prawko.prawko_server.config.IsSelfOrAdmin;

import java.io.Serial;
import java.util.Collection;

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

    /**
     * Creates a principal for an existing user.
     *
     * @param id          the ID of the user
     * @param username    the username of the user
     * @param password    the encoded password of the user
     * @param authorities the authorities granted to the user
     */
    public AuthenticatedUser(final long id,
                             final String username,
                             final String password,
                             final Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
    }

    /**
     * Returns the ID of the authenticated user.
     *
     * @return the user's ID
     */
    public long getId() {
        return id;
    }

    /**
     * Checks whether this principal is the user with the given ID.
     *
     * @param userId the ID of the user being accessed
     * @return {@code true} if this principal has the given ID
     */
    public boolean isSelf(final long userId) {
        return id == userId;
    }

}
