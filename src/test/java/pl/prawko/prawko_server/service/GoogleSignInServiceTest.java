package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtException;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.exception.ProviderNotConfiguredException;
import pl.prawko.prawko_server.service.implementation.GoogleSignInService;
import pl.prawko.prawko_server.test_data.GoogleTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GoogleSignInServiceTest {

    private static final String UNREACHABLE_JWKS = "http://localhost:1/oauth2/v3/certs";

    @Mock
    private IUserIdentityService userIdentityService;

    @Mock
    private IRefreshTokenService refreshTokenService;

    @Test
    void signIn_throwProviderNotConfigured_whenNoClientIdsAreConfigured() {
        final var service = createService(List.of());
        final var idToken = GoogleTestData.idToken(GoogleTestData.createPippinClaims());

        assertThatThrownBy(() -> service.signIn(idToken))
                .isInstanceOf(ProviderNotConfiguredException.class)
                .hasMessage(GoogleTestData.GOOGLE_NOT_AVAILABLE);
        verifyNoInteractions(userIdentityService, refreshTokenService);
    }

    @Test
    void signIn_throwJwtException_whenJwksIsUnavailable() {
        final var service = createService(List.of(GoogleTestData.WEB_CLIENT_ID));
        final var idToken = GoogleTestData.idToken(GoogleTestData.createPippinClaims());

        assertThatThrownBy(() -> service.signIn(idToken))
                .isInstanceOf(JwtException.class)
                .isNotInstanceOf(BadJwtException.class);
        verifyNoInteractions(userIdentityService, refreshTokenService);
    }

    private GoogleSignInService createService(final List<String> clientIds) {
        return new GoogleSignInService(
                clientIds, UNREACHABLE_JWKS, TestUtils.CLOCK, userIdentityService, refreshTokenService);
    }

}
