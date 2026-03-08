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
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.dto.UserUpdateRequest;
import pl.prawko.prawko_server.exception.AlreadyExistsException;
import pl.prawko.prawko_server.mapper.UserMapper;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.IUserService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
    public long register(@NonNull final RegisterDto dto) {
        log.info("Attempting to register new user: {}", dto.userName());
        validateNoConflict(dto.userName(), dto.email());
        final var user = mapper.fromDto(dto);
        repository.save(user);
        log.info("User {} registered successfully.", user.getUserName());
        return user.getId();
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

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException if a user with provided id have not been found
     */
    @Override
    public User getById(final long userId) {
        log.info("Fetching user by id: {}", userId);
        return repository.findById(userId)
                .orElseThrow(() -> {
                    final var message = "User with id '" + userId + "' not found.";
                    log.warn(message);
                    return new EntityNotFoundException(message);
                });
    }

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException if a user with provided id have not been found
     */
    @Override
    public UserDto getUserDtoById(final long userId) {
        log.info("Fetching userDto by id: {}", userId);
        return mapper.toDto(getById(userId));
    }

    @Override
    public List<UserDto> getAllUsers() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    @Override
    public UserDto updateUser(long userId, @NonNull final UserUpdateRequest updateRequest) {
        log.info("Updating user with id '{}' using: {}", userId, updateRequest);
        final var user = getById(userId);
        validateNoConflict(updateRequest.userName(), updateRequest.email());
        Optional.ofNullable(updateRequest.firstName()).ifPresent(user::setFirstName);
        Optional.ofNullable(updateRequest.lastName()).ifPresent(user::setLastName);
        Optional.ofNullable(updateRequest.userName()).ifPresent(user::setUserName);
        Optional.ofNullable(updateRequest.email()).ifPresent(user::setEmail);
        final var updated = repository.save(user);
        log.info("Successfully updated user '{}'", user.getUserName());
        return mapper.toDto(updated);
    }

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException if a user with provided id have not been found
     */
    @Transactional
    @Override
    public void deleteUser(final long userId) {
        log.info("Deleting user with id: {}", userId);
        final var user = getById(userId);
        repository.delete(user);
        log.info("Successfully deleted user '{}'", user.getUserName());
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(@NonNull final Collection<Role> roles) {
        log.debug("Mapping {} role(s) to authorities.", roles.size());
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .toList();
    }

    private void validateNoConflict(@Nullable final String userName, @Nullable final String email) {
        log.debug("Checking if there is no other user with username '{}' or email '{}'", userName, email);
        Map<String, String> errorDetails = new HashMap<>();
        if (userName != null && repository.existsByUserName(userName)) {
            errorDetails.put("userName", "User with username '" + userName + "' already exists.");
        }
        if (email != null && repository.existsByEmail(email)) {
            errorDetails.put("email", "User with email '" + email + "' already exists.");
        }
        if (!errorDetails.isEmpty()) {
            final var message = "User already exists.";
            log.warn(message + "{}", errorDetails);
            throw new AlreadyExistsException(message, errorDetails);
        }
    }

}
