package pl.prawko.prawko_server.service;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtException;
import pl.prawko.prawko_server.config.GoogleJwksStub;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.exception.ProviderNotConfiguredException;
import pl.prawko.prawko_server.exception.ProviderUnavailableException;
import pl.prawko.prawko_server.model.EmailStatus;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.model.ProviderProfile;
import pl.prawko.prawko_server.service.implementation.GoogleProfileService;
import pl.prawko.prawko_server.test_data.GoogleTestData;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleProfileServiceTest {

    private static final String UNREACHABLE_JWKS = "http://localhost:1/oauth2/v3/certs";

    private static GoogleJwksStub jwksStub;

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
    void fetchProfile_throwProviderNotConfigured_whenNoClientIdsAreConfigured() {
        final var service = new GoogleProfileService(List.of(), UNREACHABLE_JWKS, TestUtils.CLOCK);
        final var idToken = GoogleTestData.idToken(GoogleTestData.createPippinClaims());

        assertThatThrownBy(() -> service.fetchProfile(idToken))
                .isInstanceOf(ProviderNotConfiguredException.class)
                .hasMessage(GoogleTestData.GOOGLE_NOT_AVAILABLE);
    }

    @Test
    void fetchProfile_throwProviderUnavailable_whenJwksIsUnavailable() {
        final var service = new GoogleProfileService(
                List.of(GoogleTestData.WEB_CLIENT_ID),
                UNREACHABLE_JWKS,
                TestUtils.CLOCK);
        final var idToken = GoogleTestData.idToken(GoogleTestData.createPippinClaims());

        assertThatThrownBy(() -> service.fetchProfile(idToken))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasMessage(GoogleTestData.GOOGLE_UNAVAILABLE)
                .cause()
                .isInstanceOf(JwtException.class)
                .isNotInstanceOf(BadJwtException.class);
    }

    @Test
    void fetchProfile_returnProfile_whenTokenIsValid() {
        final var pippin = UserTestData.createTestUserPippin();
        final var service = createService();

        final var result = service.fetchProfile(GoogleTestData.idToken(GoogleTestData.createPippinClaims()));

        assertThat(result).isEqualTo(new ProviderProfile(
                IdentityProvider.GOOGLE,
                UserTestData.PROVIDER_USER_ID,
                pippin.getEmail(),
                EmailStatus.AUTHORITATIVE,
                StringUtils.substringBefore(pippin.getEmail(), "@"),
                pippin.getFirstName(),
                pippin.getLastName()));
    }

    @ParameterizedTest
    @MethodSource("emailStatuses")
    void fetchProfile_returnEmailStatus_whenTokenIsValid(final JwtClaimsSet.Builder claims,
                                                         final EmailStatus status) {
        final var service = createService();

        final var result = service.fetchProfile(GoogleTestData.idToken(claims));

        assertThat(result.emailStatus()).isEqualTo(status);
    }

    private static GoogleProfileService createService() {
        return new GoogleProfileService(List.of(GoogleTestData.WEB_CLIENT_ID), jwksStub.url(), TestUtils.CLOCK);
    }

}
