package pl.prawko.prawko_server.service;

import pl.prawko.prawko_server.dto.AuthTokensDto;
import pl.prawko.prawko_server.dto.GitHubSignInRequest;
import pl.prawko.prawko_server.exception.InvalidProviderCredentialException;
import pl.prawko.prawko_server.exception.ProviderNotConfiguredException;
import pl.prawko.prawko_server.exception.ProviderUnavailableException;

/**
 * Service interface for signing in with a GitHub account.
 */
public interface IGitHubSignInService {

    /**
     * Exchanges a GitHub authorization code and signs in the user linked to the GitHub account, see
     * {@link IUserIdentityService#resolveUser}.
     *
     * @param request the authorization code with the PKCE code verifier and redirect URI it was requested with
     * @return the issued tokens, as for signing in with a password
     * @throws InvalidProviderCredentialException if GitHub rejects the code or the access token issued for it
     * @throws ProviderNotConfiguredException     if the GitHub client ID or secret isn't configured
     * @throws ProviderUnavailableException       if GitHub can't be reached, answers with a server error or rejects the
     *                                            configured client credentials
     */
    AuthTokensDto signIn(GitHubSignInRequest request);

}
