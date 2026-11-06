package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for signing in with a GitHub account, or linking one.
 */
@Schema(description = "GitHub sign-in request containing the result of GitHub's authorization code flow with PKCE")
public record GitHubSignInRequest(

        @NotBlank(message = "{code.required}")
        @Schema(description = "Authorization code the client got from GitHub, requested with the scope `user:email`")
        String code,

        @NotBlank(message = "{codeverifier.required}")
        @Schema(description = "PKCE code verifier the code challenge of the authorization request was derived from")
        String codeVerifier,

        @NotBlank(message = "{redirecturi.required}")
        @Schema(description = "Redirect URI of the authorization request")
        String redirectUri

) {
}
