package pl.prawko.prawko_server.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import pl.prawko.prawko_server.exception.AlreadyExistsException;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Exceptions", description = "Controller to handle exceptions")
@RestControllerAdvice
public class ExceptionController {

    @ApiResponse(responseCode = "400", description = "File is missing")
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<String> handleMissingFile(final MissingServletRequestPartException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ApiResponse(responseCode = "415", description = "Wrong file type")
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleWrongFileType(final MultipartException exception) {
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(exception.getMessage());
    }

    @ApiResponse(responseCode = "409", description = "Entity already exists")
    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyExists(final AlreadyExistsException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        Map.of(
                                "message", exception.getMessage(),
                                "details", exception.getDetails()
                        ));
    }

    @ApiResponse(responseCode = "404", description = "Entity not found")
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFound(final EntityNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(exception.getMessage());
    }

    @ApiResponse(responseCode = "400", description = "Invalid argument")
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDto(final MethodArgumentNotValidException exception) {
        final Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "message", "Validation for request failed.",
                                "details", errors
                        ));
    }

    @ApiResponse(responseCode = "401", description = "Authentication failed")
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleInvalidLoginRequest(final AuthenticationException exception) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(exception.getMessage());
    }

    @ApiResponse(responseCode = "400", description = "ID is negative or zero")
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleNotPositiveID() {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("ID must be greater than 0.");
    }

    @ApiResponse(responseCode = "400", description = "Request body is missing")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleMissingBody() {
        return ResponseEntity
                .badRequest()
                .body("Request body is missing.");
    }

}
