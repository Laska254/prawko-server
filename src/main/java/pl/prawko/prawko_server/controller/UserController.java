package pl.prawko.prawko_server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import pl.prawko.prawko_server.dto.DeleteAccountRequest;
import pl.prawko.prawko_server.dto.LinkGitHubRequest;
import pl.prawko.prawko_server.dto.LinkGoogleRequest;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UnlinkIdentityRequest;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.dto.UserStatsDto;
import pl.prawko.prawko_server.dto.UserUpdateRequest;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.service.implementation.ExamService;
import pl.prawko.prawko_server.service.implementation.GitHubProfileService;
import pl.prawko.prawko_server.service.implementation.GoogleProfileService;
import pl.prawko.prawko_server.service.implementation.UserIdentityService;
import pl.prawko.prawko_server.service.implementation.UserService;

/**
 * REST controller for user management operations.
 *
 * <p>Provides endpoints for user registration, retrieval, updating, and deletion, and for linking and unlinking accounts
 * at identity providers.
 */
@Tag(name = "Users", description = "User management endpoints")
@Validated
@RestController
@RequestMapping(ApiConstants.USERS_BASE_URL)
public class UserController {

    private final UserService userService;
    private final ExamService examService;
    private final GoogleProfileService googleProfileService;
    private final GitHubProfileService gitHubProfileService;
    private final UserIdentityService userIdentityService;

    public UserController(final UserService userService,
                          final ExamService examService,
                          final GoogleProfileService googleProfileService,
                          final GitHubProfileService gitHubProfileService,
                          final UserIdentityService userIdentityService) {
        this.userService = userService;
        this.examService = examService;
        this.googleProfileService = googleProfileService;
        this.gitHubProfileService = gitHubProfileService;
        this.userIdentityService = userIdentityService;
    }

    /**
     * Registers a new user in the system.
     *
     * <p>The account can't be used until its email address is verified with the link emailed to it.
     *
     * @param dto the {@link RegisterDto} containing registration data
     * @return a {@link ResponseEntity} with HTTP 201 Created and Location header pointing
     * to the newly created user
     */
    @Operation(summary = "Register a new user",
            description = "Creates a new user account with the provided information data and emails a verification link.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully, email verification pending"),
            @ApiResponse(responseCode = "400", description = "Invalid argument"),
            @ApiResponse(responseCode = "409", description = "User already exists"),
            @ApiResponse(responseCode = "503", description = "Too many requests pending")
    })
    @SecurityRequirements
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
     * Retrieves the currently authenticated user.
     *
     * <p>Lets clients find out the ID and role of the account whose credentials they hold.
     *
     * @param userId the ID of the currently authenticated user
     * @return a {@link ResponseEntity} containing the {@link UserDto}
     */
    @Operation(summary = "Get current user", description = "Retrieves profile information of the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current user"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping(ApiConstants.ME)
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal(expression = "id") final long userId) {
        return ResponseEntity.ok(userService.getUserDtoById(userId));
    }

    /**
     * Retrieves exam statistics of the currently authenticated user.
     *
     * <p>Only finished exams are counted, overall and per category.
     *
     * @param userId the ID of the currently authenticated user
     * @return a {@link ResponseEntity} containing the {@link UserStatsDto}
     */
    @Operation(summary = "Get own exam statistics",
            description = "Retrieves statistics of the authenticated user's finished exams, overall and per category.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exam statistics, zero counts without finished exams"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping(ApiConstants.STATS)
    public ResponseEntity<UserStatsDto> getCurrentUserStats(@AuthenticationPrincipal(expression = "id") final long userId) {
        return ResponseEntity.ok(examService.getStats(userId));
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
     * <p>Allowed only for the user themselves or an admin, but only the user themselves may change the email: an
     * admin can't confirm who's asking, so letting them would make a stolen admin account worth every account.
     * A new email is only stored as pending and links are emailed to it and to the current address, both must
     * confirm it. Sending the current email cancels a pending change.
     *
     * @param id            the unique identifier of the user to update (must be positive)
     * @param updateRequest the {@link UserUpdateRequest} containing fields to update
     * @return a {@link ResponseEntity} containing the updated {@link UserDto}
     */
    @Operation(summary = "Update user details", description = "Updates specified fields of an existing user. "
            + "Only the user themselves may change the email. A new email takes effect only after it's verified with "
            + "the link emailed to it and confirmed with the link emailed to the current address. "
            + "Sending the current email cancels a pending change and invalidates its link.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully; a new email stays pending (`pendingEmail`) until verified"),
            @ApiResponse(responseCode = "403", description = "Updating another user, or another user's email as an admin, is not allowed"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid update data or ID is negative or zero"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists"),
            @ApiResponse(responseCode = "429", description = "Email changed again within the verification email cooldown"),
            @ApiResponse(responseCode = "503", description = "Too many requests pending")
    })
    @PreAuthorize("principal.isSelf(#id) or (hasRole('ADMIN') and #updateRequest.email() == null)")
    @PatchMapping(ApiConstants.BY_ID)
    public ResponseEntity<UserDto> updateUser(@PathVariable @Positive final long id,
                                              @Valid @RequestBody final UserUpdateRequest updateRequest) {
        return ResponseEntity.ok(userService.updateUser(id, updateRequest));
    }

    /**
     * Changes password of the currently authenticated user.
     *
     * @param userId  the ID of the currently authenticated user
     * @param request the {@link ChangePasswordRequest} containing current and new password
     * @return a {@link ResponseEntity} with HTTP 204 No Content
     */
    @Operation(summary = "Change own password", description = "Changes password of the authenticated user after verifying the current one.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or incorrect current password")
    })
    @PatchMapping(ApiConstants.PASSWORD)
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal(expression = "id") final long userId,
                                               @Valid @RequestBody final ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes the account of the currently authenticated user.
     *
     * <p>Requires re-entering the current password to confirm the deletion.
     *
     * @param userId  the ID of the currently authenticated user
     * @param request the {@link DeleteAccountRequest} containing the current password
     * @return a {@link ResponseEntity} with HTTP 204 No Content
     */
    @Operation(summary = "Delete own account", description = "Permanently removes the authenticated user after verifying their password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or incorrect password")
    })
    @DeleteMapping(ApiConstants.ME)
    public ResponseEntity<Void> deleteOwnAccount(@AuthenticationPrincipal(expression = "id") final long userId,
                                                 @Valid @RequestBody final DeleteAccountRequest request) {
        userService.deleteOwnAccount(userId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Links a Google account to the currently authenticated user, who can then sign in with it.
     *
     * <p>The account's email doesn't have to match the user's. Requires re-entering the current password if the user
     * has one. A notice is emailed to the user's current address.
     *
     * @param userId  the ID of the currently authenticated user
     * @param request the {@link LinkGoogleRequest} containing the ID token and the current password
     * @return a {@link ResponseEntity} with HTTP 204 No Content
     */
    @Operation(summary = "Link a Google account",
            description = "Verifies the current password, if the user has one, and a Google ID token, and links its "
                    + "account to the authenticated user. The account's email doesn't have to match the user's.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Google account linked successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid argument, or missing or incorrect current password"),
            @ApiResponse(responseCode = "401", description = "Not authenticated, or invalid or expired ID token, or issued for another client"),
            @ApiResponse(responseCode = "404", description = "Sign-in with Google is not configured"),
            @ApiResponse(responseCode = "409", description = "The Google account is linked to another user, or the user already has one linked"),
            @ApiResponse(responseCode = "503", description = "Google can't be reached, or too many requests pending, try again later")
    })
    @PostMapping(ApiConstants.IDENTITIES + ApiConstants.GOOGLE)
    public ResponseEntity<Void> linkGoogle(@AuthenticationPrincipal(expression = "id") final long userId,
                                           @Valid @RequestBody final LinkGoogleRequest request) {
        userService.verifyPasswordIfSet(userId, request.currentPassword());
        userIdentityService.linkIdentity(userId, googleProfileService.fetchProfile(request.idToken()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Links a GitHub account to the currently authenticated user, who can then sign in with it.
     *
     * <p>The account's email doesn't have to match the user's. Requires re-entering the current password if the user
     * has one, checked first so that a wrong one doesn't use up the code, which works only once. A notice is emailed
     * to the user's current address.
     *
     * @param userId  the ID of the currently authenticated user
     * @param request the {@link LinkGitHubRequest} containing the code, its PKCE verifier and redirect URI, and the
     *                current password
     * @return a {@link ResponseEntity} with HTTP 204 No Content
     */
    @Operation(summary = "Link a GitHub account",
            description = "Verifies the current password, if the user has one, exchanges a GitHub authorization code and "
                    + "links its account to the authenticated user. The account's email doesn't have to match the user's.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "GitHub account linked successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid argument, or missing or incorrect current password"),
            @ApiResponse(responseCode = "401", description = "Not authenticated, or invalid or used authorization code"),
            @ApiResponse(responseCode = "403", description = "GitHub refused the code over an unverified primary email"),
            @ApiResponse(responseCode = "404", description = "Sign-in with GitHub is not configured"),
            @ApiResponse(responseCode = "409", description = "The GitHub account is linked to another user, or the user already has one linked"),
            @ApiResponse(responseCode = "503", description = "GitHub can't be reached or rate limits the app, or too many requests pending, try again later")
    })
    @PostMapping(ApiConstants.IDENTITIES + ApiConstants.GITHUB)
    public ResponseEntity<Void> linkGitHub(@AuthenticationPrincipal(expression = "id") final long userId,
                                           @Valid @RequestBody final LinkGitHubRequest request) {
        userService.verifyPasswordIfSet(userId, request.currentPassword());
        userIdentityService.linkIdentity(userId, gitHubProfileService.fetchProfile(request.toSignInRequest()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Unlinks the Google account of the currently authenticated user.
     *
     * <p>Requires re-entering the current password if the user has one. A notice is emailed to the user's current
     * address.
     *
     * @param userId  the ID of the currently authenticated user
     * @param request the {@link UnlinkIdentityRequest} containing the current password, optional without one
     * @return a {@link ResponseEntity} with HTTP 204 No Content
     */
    @Operation(summary = "Unlink the Google account",
            description = "Verifies the current password, if the user has one, and unlinks the Google account of the "
                    + "authenticated user, unless it's their only way to sign in.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Google account unlinked successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or incorrect current password"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "The user has no Google account linked"),
            @ApiResponse(responseCode = "409", description = "The user has no password and no other linked account"),
            @ApiResponse(responseCode = "503", description = "Too many requests pending")
    })
    @DeleteMapping(ApiConstants.IDENTITIES + ApiConstants.GOOGLE)
    public ResponseEntity<Void> unlinkGoogle(@AuthenticationPrincipal(expression = "id") final long userId,
                                             @RequestBody(required = false) @Nullable final UnlinkIdentityRequest request) {
        return unlink(userId, IdentityProvider.GOOGLE, request);
    }

    /**
     * Unlinks the GitHub account of the currently authenticated user.
     *
     * <p>Requires re-entering the current password if the user has one. A notice is emailed to the user's current
     * address.
     *
     * @param userId  the ID of the currently authenticated user
     * @param request the {@link UnlinkIdentityRequest} containing the current password, optional without one
     * @return a {@link ResponseEntity} with HTTP 204 No Content
     */
    @Operation(summary = "Unlink the GitHub account",
            description = "Verifies the current password, if the user has one, and unlinks the GitHub account of the "
                    + "authenticated user, unless it's their only way to sign in.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "GitHub account unlinked successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or incorrect current password"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "The user has no GitHub account linked"),
            @ApiResponse(responseCode = "409", description = "The user has no password and no other linked account"),
            @ApiResponse(responseCode = "503", description = "Too many requests pending")
    })
    @DeleteMapping(ApiConstants.IDENTITIES + ApiConstants.GITHUB)
    public ResponseEntity<Void> unlinkGitHub(@AuthenticationPrincipal(expression = "id") final long userId,
                                             @RequestBody(required = false) @Nullable final UnlinkIdentityRequest request) {
        return unlink(userId, IdentityProvider.GITHUB, request);
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

    private ResponseEntity<Void> unlink(final long userId,
                                        final IdentityProvider provider,
                                        @Nullable final UnlinkIdentityRequest request) {
        userService.verifyPasswordIfSet(userId, request == null ? null : request.currentPassword());
        userIdentityService.unlinkIdentity(userId, provider);
        return ResponseEntity.noContent().build();
    }

}
