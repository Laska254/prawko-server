package pl.prawko.prawko_server.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import pl.prawko.prawko_server.exception.AlreadyExistsException;

import java.util.HashMap;

/**
 * Centralized exception handler for the entire REST API.
 */
@Tag(name = "Exceptions", description = "Controller to handle exceptions")
@RestControllerAdvice
public class ExceptionController {

    @ApiResponse(responseCode = "400", description = "File is missing")
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingFile(final MissingServletRequestPartException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ApiResponse(responseCode = "415", description = "Wrong file type")
    @ExceptionHandler(MultipartException.class)
    public ProblemDetail handleWrongFileType(final MultipartException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getMessage());
    }

    @ApiResponse(responseCode = "409", description = "Entity already exists")
    @ExceptionHandler(AlreadyExistsException.class)
    public ProblemDetail handleAlreadyExists(final AlreadyExistsException exception) {
        final var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setProperty("details", exception.getDetails());
        return problemDetail;
    }

    @ApiResponse(responseCode = "404", description = "Entity not found")
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFound(final EntityNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ApiResponse(responseCode = "400", description = "Invalid argument")
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleInvalidDto(final MethodArgumentNotValidException exception) {
        final var errors = new HashMap<>();
        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        final var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation for request failed.");
        problemDetail.setProperty("details", errors);
        return problemDetail;
    }

    @ApiResponse(responseCode = "401", description = "Authentication failed")
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleInvalidLoginRequest(final AuthenticationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ApiResponse(responseCode = "400", description = "ID is negative or zero")
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleNotPositiveID() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "ID must be greater than 0.");
    }

    @ApiResponse(responseCode = "400", description = "Request body is missing")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMissingBody() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request body is missing.");
    }

}
