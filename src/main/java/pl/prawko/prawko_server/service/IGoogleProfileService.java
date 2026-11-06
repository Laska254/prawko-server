package pl.prawko.prawko_server.service;

import pl.prawko.prawko_server.exception.InvalidProviderCredentialException;
import pl.prawko.prawko_server.exception.ProviderNotConfiguredException;
import pl.prawko.prawko_server.exception.ProviderUnavailableException;
import pl.prawko.prawko_server.model.ProviderProfile;

/**
 * Service interface for reading the profile of a Google account from a credential the client got from Google.
 */
public interface IGoogleProfileService {

    /**
     * Verifies a Google ID token and reads the profile of its Google account.
     *
     * @param idToken the ID token the client got from Google
     * @return the profile of the Google account
     * @throws InvalidProviderCredentialException if the token is malformed, expired, not signed by Google or issued
     *                                            for another client
     * @throws ProviderNotConfiguredException     if no Google client IDs are configured
     * @throws ProviderUnavailableException       if the token can't be verified, e.g. Google's keys can't be fetched
     */
    ProviderProfile fetchProfile(String idToken);

}
