package pl.prawko.prawko_server.service;

import org.springframework.security.oauth2.jwt.Jwt;
import pl.prawko.prawko_server.config.AuthenticatedUser;

/**
 * Service interface for issuing short-lived access tokens, sent by clients as {@code Authorization: Bearer ...}.
 */
public interface IAccessTokenService {

    /**
     * Issues a signed access token authenticating the given user until it expires.
     *
     * @param user the authenticated user the token is issued for
     * @return the issued token, with its value and expiry
     */
    Jwt issue(AuthenticatedUser user);

}
