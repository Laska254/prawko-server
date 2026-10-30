package pl.prawko.prawko_server.test_data;

import pl.prawko.prawko_server.model.RefreshToken;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.util.SecureTokens;

import java.time.Duration;
import java.time.Instant;

public class RefreshTokenTestData {

    public static final String TOKEN = "refreshToken";
    public static final String INVALID_REFRESH_TOKEN = "Refresh token is invalid, expired or revoked.";
    public static final Duration VALIDITY = Duration.ofDays(30);

    private RefreshTokenTestData() {
    }

    public static RefreshToken createRefreshToken(final User user, final String token, final Instant expires) {
        return new RefreshToken()
                .setTokenHash(SecureTokens.hash(token))
                .setUser(user)
                .setExpires(expires);
    }

}
