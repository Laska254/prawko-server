package pl.prawko.prawko_server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.dto.UserUpdateRequest;
import pl.prawko.prawko_server.service.implementation.UserService;

import java.util.List;

@Tag(name = "Users", description = "User management endpoints")
@Validated
@RestController
@RequestMapping(ApiConstants.USERS_BASE_URL)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @PostMapping
    public ResponseEntity<Void> registerUser(@Valid @RequestBody final RegisterDto dto) {
        final var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(ApiConstants.BY_ID)
                .buildAndExpand(userService.register(dto))
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Get user by ID")
    @ApiResponse(responseCode = "200", description = "User found")
    @GetMapping(ApiConstants.BY_ID)
    public ResponseEntity<UserDto> getUserById(@PathVariable @Positive final long id) {
        return ResponseEntity.ok(userService.getUserDtoById(id));
    }

    @Operation(summary = "Get all users")
    @ApiResponse(responseCode = "200", description = "List of all users")
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Update user details")
    @ApiResponse(responseCode = "200", description = "User updated")
    @PatchMapping(ApiConstants.BY_ID)
    public ResponseEntity<UserDto> updateUser(@PathVariable @Positive final long id,
                                              @Valid @RequestBody final UserUpdateRequest updateRequest) {
        return ResponseEntity.ok(userService.updateUser(id, updateRequest));
    }

    @Operation(summary = "Delete a user by ID")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @DeleteMapping(ApiConstants.BY_ID)
    public ResponseEntity<Void> deleteUser(@PathVariable @Positive final long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
