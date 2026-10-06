package pl.prawko.prawko_server.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pl.prawko.prawko_server.dto.ChangePasswordRequest;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.dto.UserUpdateRequest;
import pl.prawko.prawko_server.model.User;

/**
 * Service interface for managing {@link User} entities.
 */
public interface IUserService {

    /**
     * Registers a new {@link User} with the provided registration details.
     * <p>
     * Creates a new user account from registration data and persists it to the database.
     * </p>
     *
     * @param dto the registration data containing username, email, password, and personal info
     * @return the ID of the newly created user
     */
    long register(RegisterDto dto);

    /**
     * Retrieves a {@link User} by its ID.
     * <p>
     * Returns the complete user entity with all associated data.
     * </p>
     *
     * @param userId the ID of the user to retrieve
     * @return the {@link User} with the specified ID
     */
    User getById(long userId);

    /**
     * Retrieves a user's data as a {@link UserDto} by user ID.
     * <p>
     * Returns user information in data transfer object format, suitable for API responses.
     * </p>
     *
     * @param userId the ID of the user to retrieve
     * @return a {@link UserDto} containing the user's data
     */
    UserDto getUserDtoById(long userId);

    /**
     * Retrieves a page of users in the application.
     * <p>
     * Returns registered users converted to DTO format.
     * </p>
     *
     * @param pageable the pagination and sorting information
     * @return a page of users as {@link UserDto} objects
     */
    Page<UserDto> getAllUsers(Pageable pageable);

    /**
     * Updates an existing user's details.
     * <p>
     * Modifies user information such as firstname, lastname, username, and email address.
     * Validation ensures no conflicts with other users' data.
     * Changing the email invalidates any pending password reset token.
     * </p>
     *
     * @param userId        the ID of the user to update
     * @param updateRequest the request containing new user details
     * @return the updated user as a {@link UserDto}
     */
    UserDto updateUser(long userId, UserUpdateRequest updateRequest);

    /**
     * Changes a user's password.
     * <p>
     * Verifies the current password before encoding and persisting the new one.
     * Invalidates any pending password reset token.
     * </p>
     *
     * @param userId  the ID of the user whose password should be changed
     * @param request the request containing current and new password
     */
    void changePassword(long userId, ChangePasswordRequest request);

    /**
     * Deletes a user from the system.
     * <p>
     * Removes the user and all associated data from the database.
     * </p>
     *
     * @param userId the ID of the user to delete
     */
    void deleteUser(long userId);

}
