package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtException;
import pl.prawko.prawko_server.config.GoogleJwksStub;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.exception.ProviderNotConfiguredException;
import pl.prawko.prawko_server.exception.ProviderUnavailableException;
import pl.prawko.prawko_server.model.EmailStatus;
import pl.prawko.prawko_server.model.ProviderProfile;
import pl.prawko.prawko_server.service.implementation.GoogleSignInService;
import pl.prawko.prawko_server.test_data.GoogleTestData;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleSignInServiceTest {

    private static final String UNREACHABLE_JWKS = "http://localhost:1/oauth2/v3/certs";

    private static GoogleJwksStub jwksStub;

    @Mock
    private IUserIdentityService userIdentityService;

    @Mock
    private IRefreshTokenService refreshTokenService;

    @BeforeAll
    static void startJwksStub() throws IOException {
        jwksStub = new GoogleJwksStub(GoogleTestData.jwks());
    }

    @AfterAll
    static void stopJwksStub() {
        jwksStub.close();
    }

    private static Stream<Arguments> emailStatuses() {
        return Stream.of(
                Arguments.of(Named.of("Workspace email", GoogleTestData.createPippinClaims()),
                        EmailStatus.AUTHORITATIVE),
                Arguments.of(Named.of("Gmail address", withoutHostedDomain("pippin@gmail.com")),
                        EmailStatus.AUTHORITATIVE),
                Arguments.of(Named.of("Gmail address in upper case", withoutHostedDomain("PIPPIN@GMAIL.COM")),
                        EmailStatus.AUTHORITATIVE),
                Arguments.of(Named.of("other email", withoutHostedDomain(UserTestData.createTestUserPippin().getEmail())),
                        EmailStatus.VERIFIED),
                Arguments.of(Named.of("only ending like Gmail", withoutHostedDomain("pippin@notgmail.com")),
                        EmailStatus.VERIFIED),
                Arguments.of(Named.of("blank hosted domain", GoogleTestData.createPippinClaims()
                        .claim(GoogleTestData.HOSTED_DOMAIN, " ")), EmailStatus.VERIFIED),
                Arguments.of(Named.of("unverified Workspace email", GoogleTestData.createPippinClaims()
                        .claim(StandardClaimNames.EMAIL_VERIFIED, false)), EmailStatus.UNVERIFIED),
                Arguments.of(Named.of("without verification claim", GoogleTestData.createPippinClaims()
                        .claims(claims -> claims.remove(StandardClaimNames.EMAIL_VERIFIED))), EmailStatus.UNVERIFIED),
                Arguments.of(Named.of("without email", GoogleTestData.createPippinClaims()
                        .claims(claims -> claims.remove(StandardClaimNames.EMAIL))), EmailStatus.UNVERIFIED)
        );
    }

    private static JwtClaimsSet.Builder withoutHostedDomain(final String email) {
        return GoogleTestData.createPippinClaims()
                .claim(StandardClaimNames.EMAIL, email)
                .claims(claims -> claims.remove(GoogleTestData.HOSTED_DOMAIN));
    }

    @Test
    void signIn_throwProviderNotConfigured_whenNoClientIdsAreConfigured() {
        final var service = createService(List.of(), UNREACHABLE_JWKS);
        final var idToken = GoogleTestData.idToken(GoogleTestData.createPippinClaims());

        assertThatThrownBy(() -> service.signIn(idToken))
                .isInstanceOf(ProviderNotConfiguredException.class)
                .hasMessage(GoogleTestData.GOOGLE_NOT_AVAILABLE);
        verifyNoInteractions(userIdentityService, refreshTokenService);
    }

    @Test
    void signIn_throwProviderUnavailable_whenJwksIsUnavailable() {
        final var service = createService(List.of(GoogleTestData.WEB_CLIENT_ID), UNREACHABLE_JWKS);
        final var idToken = GoogleTestData.idToken(GoogleTestData.createPippinClaims());

        assertThatThrownBy(() -> service.signIn(idToken))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasMessage(GoogleTestData.GOOGLE_UNAVAILABLE)
                .cause()
                .isInstanceOf(JwtException.class)
                .isNotInstanceOf(BadJwtException.class);
        verifyNoInteractions(userIdentityService, refreshTokenService);
    }

    @ParameterizedTest
    @MethodSource("emailStatuses")
    void signIn_resolveUserWithEmailStatus_whenTokenIsValid(final JwtClaimsSet.Builder claims,
                                                            final EmailStatus status) {
        final var service = createService(List.of(GoogleTestData.WEB_CLIENT_ID), jwksStub.url());
        final var profile = ArgumentCaptor.forClass(ProviderProfile.class);
        when(userIdentityService.resolveUser(any())).thenReturn(UserTestData.createTestUserPippin().setId(7L));

        service.signIn(GoogleTestData.idToken(claims));

        verify(userIdentityService).resolveUser(profile.capture());
        assertThat(profile.getValue().emailStatus()).isEqualTo(status);
    }

    private GoogleSignInService createService(final List<String> clientIds, final String jwkSetUri) {
        return new GoogleSignInService(clientIds, jwkSetUri, TestUtils.CLOCK, userIdentityService, refreshTokenService);
    }

}
