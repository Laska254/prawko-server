package pl.prawko.prawko_server.service.implementation;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.repository.RoleRepository;
import pl.prawko.prawko_server.service.IRoleService;

/**
 * Implementation of {@link IRoleService} that manages {@link Role} entities.
 */
@Service
public class RoleService implements IRoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    @NonNull
    private final RoleRepository repository;

    public RoleService(@NonNull final RoleRepository repository) {
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException when role doesn't exists
     */
    @Nullable
    @Override
    public Role getByName(@NonNull final String name) {
        log.debug("Fetching role '{}'", name);
        return repository.findByName(name)
                .orElseThrow(() -> {
                    log.debug("Role not found. {}", name);
                    return new EntityNotFoundException("Role with name '" + name + "' not found.");
                });
    }

}
