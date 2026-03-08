package pl.prawko.prawko_server.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import pl.prawko.prawko_server.dto.ApiResponse;
import pl.prawko.prawko_server.exception.AlreadyExistsException;

import java.util.HashMap;
import java.util.Map;

/**
 * Handle exceptions in REST controllers.
 * <p>
 * Returns error responses using {@link ApiResponse} with {@code HttpStatus}
 */
@RestControllerAdvice
public class ExceptionController {

    /**
     * Handles cases when a request doesn't contain the required file.
     *
     * @param exception exception thrown when a required file is missing
     * @return an error response with 400 Bad Request
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingFile(final MissingServletRequestPartException exception) {
        return new ApiResponse<>("Required part 'file' is not present.");
    }

    /**
     * Handles cases when the request contains the wrong file type.
     *
     * @param exception exception thrown when a required file is the wrong type
     * @return an error response with 415 Unsupported Media Type
     */
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ApiResponse<Void> handleWrongFileType(final MultipartException exception) {
        return new ApiResponse<>(exception.getMessage());
    }

    /**
     * Handles cases when the entity already exists.
     *
     * @param exception exception thrown when the entity already exists
     * @return an error response with 409 Conflict
     */
    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Map<String, String>> handleAlreadyExists(final AlreadyExistsException exception) {
        return new ApiResponse<>(exception.getMessage(), exception.getDetails());
    }

    /**
     * Handles cases when the entity was not found.
     *
     * @param exception exception thrown when the entity was not found
     * @return an error response with 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleEntityNotFound(final EntityNotFoundException exception) {
        return new ApiResponse<>(exception.getMessage());
    }

    /**
     * Handles cases when the requested body is invalid.
     *
     * @param exception exception thrown when the request body contains invalid content.
     * @return an error response with 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleInvalidDto(final MethodArgumentNotValidException exception) {
        final Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return new ApiResponse<>("Validation for request failed.", errors);
    }

    /**
     * Handles cases when authentication using login request failed.
     *
     * @param exception exception thrown when login credentials are invalid.
     * @return an error response with 400 Bad Request
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInvalidLoginRequest(final AuthenticationException exception) {
        return new ApiResponse<>("Invalid login or password.");
    }

}
