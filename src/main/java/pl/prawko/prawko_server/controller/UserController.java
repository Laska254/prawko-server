package pl.prawko.prawko_server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import pl.prawko.prawko_server.dto.ChangePasswordRequest;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.dto.UserUpdateRequest;
import pl.prawko.prawko_server.service.implementation.UserService;

import java.security.Principal;

/**
 * REST controller for user management operations.
 *
 * <p>Provides endpoints for user registration, retrieval, updating, and deletion.
 */
@Tag(name = "Users", description = "User management endpoints")
@Validated
@RestController
@RequestMapping(ApiConstants.USERS_BASE_URL)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Registers a new user in the system.
     *
     * @param dto the {@link RegisterDto} containing registration data
     * @return a {@link ResponseEntity} with HTTP 201 Created and Location header pointing
     * to the newly created user
     */
    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided information data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid argument"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    @PostMapping
    public ResponseEntity<Void> registerUser(@Valid @RequestBody final RegisterDto dto) {
        final var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(ApiConstants.BY_ID)
                .buildAndExpand(userService.register(dto))
                .toUri();
        return ResponseEntity.created(location).build();
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id the unique identifier of the user
     * @return a {@link ResponseEntity} containing the {@link UserDto}
     */
    @Operation(summary = "Get user by ID", description = "Retrieves a user's profile information by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping(ApiConstants.BY_ID)
    public ResponseEntity<UserDto> getUserById(@PathVariable @Positive final long id) {
        return ResponseEntity.ok(userService.getUserDtoById(id));
    }

    /**
     * Retrieves a page of users in the system.
     *
     * <p>Sorted by ID ascending unless specified otherwise.
     *
     * @param pageable the pagination and sorting information
     * @return a {@link ResponseEntity} containing a {@link PagedModel} of {@link UserDto}'s
     */
    @Operation(summary = "Get all users", description = "Retrieves a page of registered users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of users"),
            @ApiResponse(responseCode = "400", description = "Invalid sort property")
    })
    @GetMapping
    public ResponseEntity<PagedModel<UserDto>> getAllUsers(
            @ParameterObject @SortDefault("id") final Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(userService.getAllUsers(pageable)));
    }

    /**
     * Updates user details.
     *
     * <p>Allowed only for the user themselves or an admin.
     *
     * @param id            the unique identifier of the user to update (must be positive)
     * @param updateRequest the {@link UserUpdateRequest} containing fields to update
     * @return a {@link ResponseEntity} containing the updated {@link UserDto}
     */
    @Operation(summary = "Update user details", description = "Updates specified fields of an existing user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "403", description = "Updating another user is not allowed"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid update data or ID is negative or zero")
    })
    @PreAuthorize("hasRole('ADMIN') or @userAuthorization.isSelf(#id, authentication)")
    @PatchMapping(ApiConstants.BY_ID)
    public ResponseEntity<UserDto> updateUser(@PathVariable @Positive final long id,
                                              @Valid @RequestBody final UserUpdateRequest updateRequest) {
        return ResponseEntity.ok(userService.updateUser(id, updateRequest));
    }

    /**
     * Changes password of the currently authenticated user.
     *
     * @param principal the currently authenticated user
     * @param request   the {@link ChangePasswordRequest} containing current and new password
     * @return a {@link ResponseEntity} with HTTP 204 No Content
     */
    @Operation(summary = "Change own password", description = "Changes password of the authenticated user after verifying the current one.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or incorrect current password")
    })
    @PatchMapping(ApiConstants.PASSWORD)
    public ResponseEntity<Void> changePassword(final Principal principal,
                                               @Valid @RequestBody final ChangePasswordRequest request) {
        userService.changePassword(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes a user from the database.
     *
     * @param id the unique identifier of the user to delete (must be positive)
     * @return a {@link ResponseEntity} with HTTP 204 No Content
     */
    @Operation(summary = "Delete a user", description = "Permanently removes a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "ID is negative or zero")
    })
    @DeleteMapping(ApiConstants.BY_ID)
    public ResponseEntity<Void> deleteUser(@PathVariable @Positive final long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
