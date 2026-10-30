package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for the tokens issued on sign-in and on refresh.
 *
 * <p>The access token is sent as {@code Authorization: Bearer ...}, the refresh token is exchanged for a new pair
 * once the access token expires and can be used only once.
 */
@Schema(description = "Access and refresh tokens of a signed-in user")
public record AuthTokensDto(

        @Schema(description = "Short-lived JWT access token, sent as `Authorization: Bearer ...`")
        String accessToken,

        @Schema(description = "Long-lived single-use token for getting a new token pair")
        String refreshToken,

        @Schema(description = "Seconds until the access token expires", example = "900")
        long expiresIn

) {
}
