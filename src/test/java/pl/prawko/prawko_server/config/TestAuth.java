package pl.prawko.prawko_server.config;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetailsService;
import pl.prawko.prawko_server.service.IAccessTokenService;

/**
 * Authenticates test requests as the {@link TestSecurityConfig} accounts with a freshly issued access token.
 * The token carries the account's ID at the time it's issued, so call it after saving the matching user.
 */
public class TestAuth {

    private final IAccessTokenService accessTokenService;
    private final UserDetailsService userDetailsService;

    public TestAuth(final IAccessTokenService accessTokenService, final UserDetailsService userDetailsService) {
        this.accessTokenService = accessTokenService;
        this.userDetailsService = userDetailsService;
    }

    public void user(final HttpHeaders headers) {
        headers.setBearerAuth(token(TestUtils.USER_NAME));
    }

    public void admin(final HttpHeaders headers) {
        headers.setBearerAuth(token(TestUtils.ADMIN_NAME));
    }

    private String token(final String username) {
        final var principal = (AuthenticatedUser) userDetailsService.loadUserByUsername(username);
        return accessTokenService.issue(principal)
                .getTokenValue();
    }

}
