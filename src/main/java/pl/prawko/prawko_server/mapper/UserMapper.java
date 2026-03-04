package pl.prawko.prawko_server.mapper;

import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.service.implementation.RoleService;

import java.util.List;

/**
 * This class is responsible for mapping {@link RegisterDto} to {@link User} entity.
 * <p>
 * The mapper is registered as a Spring Component, so it can be injected into services or other components that require user mapping functionality.
 */
@Component
public class UserMapper {

    @NonNull
    private final RoleService roleService;
    @NonNull
    private final PasswordEncoder passwordEncoder;

    public UserMapper(@NonNull final RoleService roleService,
                      @NonNull final PasswordEncoder passwordEncoder) {
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Mapping {@link RegisterDto} into {@link User} entity.
     *
     * @param dto provided dto to map user from it
     * @return mapped user with standard role and encoded password
     */
    @NonNull
    public User fromDto(@NonNull final RegisterDto dto) {
        return new User()
                .setFirstName(dto.firstName())
                .setLastName(dto.lastName())
                .setUserName(dto.userName())
                .setEmail(dto.email())
                .setPassword(passwordEncoder.encode(dto.password()))
                .setRoles(List.of(roleService.getByName("USER")))
                .setEnabled(true);
    }

    @NonNull
    public UserDto toDto(@NonNull final User user){
        return new UserDto(
          user.getId(),
          user.getFirstName(),
          user.getLastName(),
                user.getUserName(),
                user.getEmail()
        );
    }

}
