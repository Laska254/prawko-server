package pl.prawko.prawko_server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.LoginDto;

/**
 * REST controller for authentication operations.
 *
 * <p>Provides endpoints for user login and authentication management.
 * Authentication is performed using Spring Security's authentication manager,
 * and successful authentication stores the Authentication in the SecurityContext.
 */
@Tag(name = "Auth", description = "Authentication management endpoints")
@RestController
@RequestMapping(ApiConstants.AUTH_BASE_URL)
public class AuthController {

    private final AuthenticationManager authenticationManager;

    public AuthController(final AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * Authenticates a user with provided credentials.
     *
     * <p>Upon successful authentication, the user's Authentication is stored in the
     * SecurityContext for the current session, allowing subsequent requests to be
     * processed with the user's granted authorities.
     *
     * @param request the {@link LoginDto} containing username and password
     * @return a {@link ResponseEntity} with HTTP 200 Ok and success message
     */
    @Operation(summary = "Sign-in", description = "Authenticates a user with username and password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User signed-in successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication failed"),
            @ApiResponse(responseCode = "400", description = "Invalid argument"),
    })
    @PostMapping
    public ResponseEntity<String> login(@Valid @RequestBody final LoginDto request) {
        final Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.userName(),
                request.password()
        ));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return ResponseEntity.ok("User signed-in successfully.");
    }

}
