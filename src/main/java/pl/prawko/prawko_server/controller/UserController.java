package pl.prawko.prawko_server.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.prawko.prawko_server.dto.ApiResponse;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.dto.UserUpdateRequest;
import pl.prawko.prawko_server.exception.AlreadyExistsException;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.service.implementation.UserService;

import java.util.List;

/**
 * REST controller for managing {@link User} entities using http requests.
 * <p>
 * Mapped on {@code /users}.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST method to register a new {@link User}.
     * <p>
     * Errors are handled by {@link ExceptionController#handleAlreadyExists(AlreadyExistsException)}
     *
     * @param dto the DTO containing user registration details
     * @return status 200 with success message
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> registerUser(@Valid @RequestBody final RegisterDto dto) {
        userService.register(dto);
        return new ApiResponse<>("User registered successfully.");
    }

    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable final long id) {
        return userService.getUserDtoById(id);
    }

    @GetMapping
    public List<UserDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @PatchMapping("/{id}")
    public ApiResponse<UserDto> updateUser(@PathVariable final long id,
                                           @Valid @RequestBody final UserUpdateRequest updateRequest) {
        final var updatedUser = userService.updateUser(id, updateRequest);
        return new ApiResponse<>("User updated successfully.", updatedUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteUser(@PathVariable final long id) {
        userService.deleteUser(id);
        return new ApiResponse<>("User deleted successfully.");
    }

}
