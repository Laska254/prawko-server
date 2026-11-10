package pl.prawko.prawko_server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

/**
 * DTO for linking a GitHub account to the currently authenticated user.
 *
 * <p>Requires re-entering the current password if the user has one, its absence is reported like a wrong password.
 */
@Schema(description = "Request to link a GitHub account, "
        + "containing the result of GitHub's authorization code flow with PKCE")
public record LinkGitHubRequest(

        @NotBlank(message = "{code.required}")
        @Schema(description = "Authorization code the client got from GitHub, requested with the scope `user:email`")
        String code,

        @NotBlank(message = "{codeverifier.required}")
        @Schema(description = "PKCE code verifier the code challenge of the authorization request was derived from")
        String codeVerifier,

        @NotBlank(message = "{redirecturi.required}")
        @Schema(description = "Redirect URI of the authorization request")
        String redirectUri,

        @Nullable
        @Schema(description = "User's current password, required if they have one", nullable = true)
        String currentPassword

) {

    public GitHubSignInRequest toSignInRequest() {
        return new GitHubSignInRequest(code, codeVerifier, redirectUri);
    }

}
