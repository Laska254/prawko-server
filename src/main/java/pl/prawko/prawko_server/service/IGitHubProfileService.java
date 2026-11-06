package pl.prawko.prawko_server.service;

import pl.prawko.prawko_server.dto.GitHubSignInRequest;
import pl.prawko.prawko_server.exception.InvalidProviderCredentialException;
import pl.prawko.prawko_server.exception.ProviderEmailNotVerifiedException;
import pl.prawko.prawko_server.exception.ProviderNotConfiguredException;
import pl.prawko.prawko_server.exception.ProviderUnavailableException;
import pl.prawko.prawko_server.model.ProviderProfile;

/**
 * Service interface for reading the profile of a GitHub account from an authorization code the client got from GitHub.
 */
public interface IGitHubProfileService {

    /**
     * Exchanges a GitHub authorization code and reads the profile of its GitHub account.
     *
     * @param request the authorization code with the PKCE code verifier and redirect URI it was requested with
     * @return the profile of the GitHub account
     * @throws ProviderEmailNotVerifiedException  if GitHub refuses the exchange over an unverified primary email
     * @throws InvalidProviderCredentialException if GitHub rejects the code or the access token issued for it
     * @throws ProviderNotConfiguredException     if the GitHub client ID or secret isn't configured
     * @throws ProviderUnavailableException       if GitHub can't be reached, answers with a server error, rate limits
     *                                            the request or rejects the configured client credentials
     */
    ProviderProfile fetchProfile(GitHubSignInRequest request);

}
