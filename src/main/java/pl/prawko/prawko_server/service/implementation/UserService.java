package pl.prawko.prawko_server.service.implementation;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.exception.AlreadyExistsException;
import pl.prawko.prawko_server.mapper.UserMapper;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.IUserService;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link IUserService} that manage users entities.
 * <p>
 * It also implements {@link UserDetailsService} for authentication purposes.
 */
@Service
public class UserService implements IUserService, UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @NonNull
    private final UserRepository repository;
    @NonNull
    private final UserMapper mapper;

    public UserService(@NonNull final UserRepository repository,
                       @NonNull final UserMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     *
     * @throws AlreadyExistsException if a user with the same username or email already exists
     */
    @Override
    @Transactional
    public void register(@NonNull final RegisterDto dto) {
        log.info("Attempting to register new user: {}", dto.userName());
        final Map<String, String> errorDetails = new LinkedHashMap<>();
        if (repository.existsByUserName(dto.userName())) {
            errorDetails.put("userName", "User with username '" + dto.userName() + "' already exists.");
        }
        if (repository.existsByEmail(dto.email())) {
            errorDetails.put("email", "User with email '" + dto.email() + "' already exists.");
        }
        if (!errorDetails.isEmpty()) {
            log.warn("Registration failed: {}", errorDetails);
            throw new AlreadyExistsException("User already exists.", errorDetails);
        }
        final var user = mapper.fromDto(dto);
        repository.save(user);
        log.info("User {} registered successfully.", user.getUserName());
    }

    /**
     * Checks if entity exists by userName or email.
     *
     * @param userNameOrEmail provided name or email to look for
     * @return {@code true} if entity exist
     */
    @Override
    public boolean checkIfExist(@NonNull final String userNameOrEmail) {
        log.debug("Checking if user exists by username or email: {}", userNameOrEmail);
        final var exists = repository.existsByUserName(userNameOrEmail) || repository.existsByEmail(userNameOrEmail);
        log.debug("User exists: {}", exists);
        return exists;
    }

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException if the user with provided userName or email doesn't exist
     */
    @Nullable
    @Override
    public User getByUserNameOrEmail(@NonNull final String userNameOrEmail) {
        log.info("Fetching user by username or email: {}", userNameOrEmail);
        return repository.findByUserNameOrEmail(userNameOrEmail)
                .orElseThrow(() -> {
                    final var message = "User with username or email '" + userNameOrEmail + "' not found.";
                    log.warn(message);
                    return new EntityNotFoundException(message);
                });
    }

    /**
     * Load user-specific data during authentication.
     * <p>
     *
     * @param userNameOrEmail the userName or email identifying the user
     * @return {@link org.springframework.security.core.userdetails.User} object with granted authorities based on user's roles
     * @throws UsernameNotFoundException if user have not been found with the provided details
     */
    @Nullable
    @Override
    public UserDetails loadUserByUsername(final String userNameOrEmail) throws UsernameNotFoundException {
        log.info("Loading user by username or email: {}", userNameOrEmail);
        if (checkIfExist(userNameOrEmail)) {
            final var user = getByUserNameOrEmail(userNameOrEmail);
            log.info("User {} loaded successfully.", userNameOrEmail);
            return new org.springframework.security.core.userdetails.User(
                    user.getUserName(),
                    user.getPassword(),
                    mapRolesToAuthorities(user.getRoles()));
        } else {
            log.warn("User '{}' not found.", userNameOrEmail);
            throw new UsernameNotFoundException("Invalid login or password.");
        }
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(@NonNull final Collection<Role> roles) {
        log.debug("Mapping {} role(s) to authorities.", roles.size());
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException if a user with provided id have not been found
     */
    @Override
    public Optional<User> getById(final long userId) {
        log.info("Fetching user by id: {}", userId);
        final Optional<User> user = repository.findById(userId);
        if (user.isEmpty()) {
            log.warn("User with id {} not found.", userId);
        }
        return user;
    }

}
