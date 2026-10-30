package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for exchanging a refresh token for a new token pair.
 */
@Schema(description = "Refresh request containing the refresh token")
public record RefreshTokenRequest(

        @NotBlank(message = "{token.required}")
        @Schema(description = "Refresh token issued on sign-in or the previous refresh")
        String refreshToken

) {
}
