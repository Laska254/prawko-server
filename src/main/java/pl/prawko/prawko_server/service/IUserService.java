package pl.prawko.prawko_server.service;

import org.jspecify.annotations.Nullable;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.dto.UserUpdateRequest;
import pl.prawko.prawko_server.model.User;

import java.util.List;

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
     * Checks if a user exists by username or email.
     * <p>
     * Used to validate user availability during registration and login operations.
     * </p>
     *
     * @param userNameOrEmail the username or email to check
     * @return {@code true} if a user with the given username or email exists, {@code false} otherwise
     */
    boolean checkIfExist(String userNameOrEmail);

    /**
     * Retrieves a {@link User} by username or email.
     * <p>
     * Searches the database for a user matching the provided username or email address.
     * </p>
     *
     * @param userNameOrEmail the username or email to search for
     * @return the {@link User} if found
     */
    @Nullable
    User getByUserNameOrEmail(String userNameOrEmail);

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
     * Retrieves all users in the application.
     * <p>
     * Returns a complete list of all registered users converted to DTO format.
     * </p>
     *
     * @return a list of all users as {@link UserDto} objects
     */
    List<UserDto> getAllUsers();

    /**
     * Updates an existing user's details.
     * <p>
     * Modifies user information such as firstname, lastname, username, and email address.
     * Validation ensures no conflicts with other users' data.
     * </p>
     *
     * @param userId        the ID of the user to update
     * @param updateRequest the request containing new user details
     * @return the updated user as a {@link UserDto}
     */
    UserDto updateUser(long userId, UserUpdateRequest updateRequest);

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
