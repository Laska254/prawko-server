package pl.prawko.prawko_server.service;

import org.jspecify.annotations.Nullable;
import pl.prawko.prawko_server.model.Role;

/**
 * Service interface for managing {@link Role} entities.
 */
public interface IRoleService {

    /**
     * Retrieves a {@link Role} by its name.
     * <p>
     * Used during user registration and role assignment to find the role entity
     * that matches the provided name.
     * </p>
     *
     * @param name the name of the role to retrieve
     * @return the {@link Role} with the specified name, or {@code null} if not found
     */
    @Nullable
    Role getByName(String name);

}
