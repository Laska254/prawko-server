package pl.prawko.prawko_server.service.implementation;

import jakarta.persistence.EntityNotFoundException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.prawko.prawko_server.config.AuthenticatedUser;
import pl.prawko.prawko_server.dto.ChangePasswordRequest;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.dto.UserUpdateRequest;
import pl.prawko.prawko_server.exception.AlreadyExistsException;
import pl.prawko.prawko_server.exception.InvalidPasswordException;
import pl.prawko.prawko_server.mapper.UserMapper;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.IUserService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link IUserService} that manages user entities.
 * <p>
 * Also implements {@link UserDetailsService} for authentication purposes.
 */
@Service
public class UserService implements IUserService, UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(final UserRepository repository,
                       final UserMapper mapper,
                       final PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * {@inheritDoc}
     *
     * @throws AlreadyExistsException if a user with the same username or email already exists
     */
    @Override
    @Transactional
    public long register(final RegisterDto dto) {
        log.info("Attempting to register new user: {}", dto.userName());
        validateNoConflict(dto.userName(), dto.email());
        log.debug("No conflicts");
        final var user = mapper.fromDto(dto);
        log.debug("Mapped successfully");
        user.setPassword(passwordEncoder.encode(dto.password()));
        log.debug("Password encoded");
        user.setRole(Role.USER);
        log.debug("User role set");
        repository.save(user);
        log.info("User {} registered successfully.", user.getUserName());
        return user.getId();
    }

    /**
     * Load user-specific data during authentication.
     *
     * @param userNameOrEmail the userName or email identifying the user
     * @return {@link AuthenticatedUser} carrying the user's ID, with granted authority based on user's role
     * @throws UsernameNotFoundException if user have not been found with the provided details
     */
    @Override
    public UserDetails loadUserByUsername(final String userNameOrEmail) throws UsernameNotFoundException {
        log.info("Loading user by username or email: {}", userNameOrEmail);
        final var user = repository.findByUserNameOrEmailIgnoreCase(userNameOrEmail, userNameOrEmail)
                .orElseThrow(() -> {
                    log.warn("User '{}' not found.", userNameOrEmail);
                    return new UsernameNotFoundException("Invalid login or password.");
                });
        log.info("User {} loaded successfully.", userNameOrEmail);
        return new AuthenticatedUser(
                user.getId(),
                user.getUserName(),
                user.getPassword(),
                AuthorityUtils.createAuthorityList(user.getRole().getAuthority()));
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
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(final Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toDto);
    }

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException if user with the given ID have not been found
     * @throws AlreadyExistsException  if the new username or email conflicts with another user
     */
    @Transactional
    @Override
    public UserDto updateUser(long userId, final UserUpdateRequest updateRequest) {
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
     * @throws EntityNotFoundException  if a user with provided id have not been found
     * @throws InvalidPasswordException if the current password doesn't match or the new password is the same as current
     */
    @Transactional
    @Override
    public void changePassword(final long userId, final ChangePasswordRequest request) {
        log.info("Changing password for user with id: {}", userId);
        final var user = getById(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            final var message = "Current password is incorrect.";
            log.warn("{} User id: {}", message, userId);
            throw new InvalidPasswordException(message);
        }
        if (request.currentPassword().equals(request.newPassword())) {
            final var message = "New password must be different from the current one.";
            log.warn("{} User id: {}", message, userId);
            throw new InvalidPasswordException(message);
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()))
                .setPasswordResetTokenHash(null)
                .setPasswordResetTokenExpires(null);
        repository.save(user);
        log.info("Successfully changed password for user '{}'", user.getUserName());
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

    private void validateNoConflict(@Nullable final String userName, @Nullable final String email) {
        log.debug("Checking if there is no other user with username '{}' or email '{}'", userName, email);
        Map<String, String> errorDetails = new HashMap<>();
        if (userName != null && repository.existsByUserName(userName)) {
            errorDetails.put("userName", "User with username '" + userName + "' already exists.");
        }
        if (email != null && repository.existsByEmailIgnoreCase(email)) {
            errorDetails.put("email", "User with email '" + email + "' already exists.");
        }
        if (!errorDetails.isEmpty()) {
            final var message = "User already exists.";
            log.warn(message + "{}", errorDetails);
            throw new AlreadyExistsException(message, errorDetails);
        }
    }

}
