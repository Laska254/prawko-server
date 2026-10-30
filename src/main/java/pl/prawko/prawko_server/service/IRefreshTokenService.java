package pl.prawko.prawko_server.service;

import pl.prawko.prawko_server.config.AuthenticatedUser;
import pl.prawko.prawko_server.dto.AuthTokensDto;
import pl.prawko.prawko_server.exception.InvalidRefreshTokenException;

/**
 * Service interface for issuing token pairs: a short-lived access token from {@link IAccessTokenService} and a
 * long-lived refresh token, exchanged for a new pair without signing in again.
 * <p>
 * Refresh tokens are rotated: each can be used once. Using a revoked token again means it leaked, so all refresh
 * tokens of its user are revoked, signing out both the legitimate client and whoever else holds one.
 */
public interface IRefreshTokenService {

    /**
     * Issues an access token and a new refresh token for the user.
     *
     * @param user the authenticated user the tokens are issued for
     * @return the issued token pair
     */
    AuthTokensDto issueTokens(AuthenticatedUser user);

    /**
     * Revokes the refresh token and issues a new pair for its user, with the user's current role.
     *
     * @param refreshToken the refresh token issued on sign-in or the previous refresh
     * @return the issued token pair
     * @throws InvalidRefreshTokenException if the token is unknown, expired or revoked; a revoked token also revokes
     *                                      all refresh tokens of its user
     */
    AuthTokensDto refresh(String refreshToken);

    /**
     * Revokes the refresh token, if it exists. Its later use is answered like an unknown token, without revoking the
     * other refresh tokens of its user.
     *
     * @param refreshToken the refresh token to revoke
     */
    void revoke(String refreshToken);

    /**
     * Deletes refresh tokens that expired, revoked or not.
     */
    void deleteExpiredTokens();

}
