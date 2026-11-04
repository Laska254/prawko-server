package pl.prawko.prawko_server.service;

import pl.prawko.prawko_server.dto.AuthTokensDto;
import pl.prawko.prawko_server.exception.InvalidProviderCredentialException;
import pl.prawko.prawko_server.exception.ProviderNotConfiguredException;
import pl.prawko.prawko_server.exception.ProviderUnavailableException;

/**
 * Service interface for signing in with a Google account.
 */
public interface IGoogleSignInService {

    /**
     * Verifies a Google ID token and signs in the user linked to the Google account, see
     * {@link IUserIdentityService#resolveUser}.
     *
     * @param idToken the ID token the client got from Google
     * @return the issued tokens, as for signing in with a password
     * @throws InvalidProviderCredentialException if the token is malformed, expired, not signed by Google or issued
     *                                            for another client
     * @throws ProviderNotConfiguredException     if no Google client IDs are configured
     * @throws ProviderUnavailableException       if the token can't be verified, e.g. Google's keys can't be fetched
     */
    AuthTokensDto signIn(String idToken);

}
