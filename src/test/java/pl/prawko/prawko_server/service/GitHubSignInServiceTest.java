package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.prawko.prawko_server.dto.AuthTokensDto;
import pl.prawko.prawko_server.exception.InvalidProviderCredentialException;
import pl.prawko.prawko_server.exception.ProviderNotConfiguredException;
import pl.prawko.prawko_server.exception.ProviderUnavailableException;
import pl.prawko.prawko_server.model.EmailStatus;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.model.ProviderProfile;
import pl.prawko.prawko_server.service.implementation.GitHubSignInService;
import pl.prawko.prawko_server.test_data.GitHubTestData;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubSignInServiceTest {

    @Mock
    private IUserIdentityService userIdentityService;

    @Mock
    private IRefreshTokenService refreshTokenService;

    private MockRestServiceServer github;
    private GitHubSignInService service;

    private static Stream<Arguments> unusableResponses() {
        return Stream.of(
                Arguments.of(Named.of("server error", MockRestResponseCreators.withServerError())),
                Arguments.of(Named.of("bad gateway", MockRestResponseCreators.withStatus(HttpStatus.BAD_GATEWAY))),
                Arguments.of(Named.of("connection failure",
                        MockRestResponseCreators.withException(new IOException("connection refused")))),
                Arguments.of(Named.of("malformed body", GitHubTestData.json("{not json"))),
                Arguments.of(Named.of("empty body", MockRestResponseCreators.withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)))
        );
    }

    private static Stream<Arguments> emails() {
        final var email = UserTestData.createTestUserPippin().getEmail();
        return Stream.of(
                Arguments.of(Named.of("primary verified", GitHubTestData.emailsJson(true)),
                        email, EmailStatus.VERIFIED),
                Arguments.of(Named.of("primary not verified", GitHubTestData.emailsJson(false)),
                        email, EmailStatus.UNVERIFIED),
                Arguments.of(Named.of("no primary", """
                        [{"email": "other@shire.me", "primary": false, "verified": true}]"""),
                        null, EmailStatus.UNVERIFIED),
                Arguments.of(Named.of("no emails", "[]"), null, EmailStatus.UNVERIFIED)
        );
    }

    @BeforeEach
    void setUp() {
        final var builder = RestClient.builder();
        github = MockRestServiceServer.bindTo(builder).build();
        service = createService(GitHubTestData.CLIENT_ID, GitHubTestData.CLIENT_SECRET, builder);
    }

    @ParameterizedTest
    @CsvSource(value = {"'', secret", "id, ''", "'', ''"})
    void signIn_throwProviderNotConfigured_whenClientCredentialsAreMissing(final String clientId,
                                                                           final String clientSecret) {
        final var builder = RestClient.builder();
        final var server = MockRestServiceServer.bindTo(builder).build();
        final var request = GitHubTestData.createValidRequest();
        final var disabled = createService(clientId, clientSecret, builder);

        assertThatThrownBy(() -> disabled.signIn(request))
                .isInstanceOf(ProviderNotConfiguredException.class)
                .hasMessage(GitHubTestData.GITHUB_NOT_AVAILABLE);
        server.verify();
        verifyNoInteractions(userIdentityService, refreshTokenService);
    }

    @Test
    void signIn_throwInvalidCredential_whenGitHubRejectsCode() {
        GitHubTestData.expectTokenExchange(github, GitHubTestData.json(GitHubTestData.BAD_CODE_RESPONSE));

        assertSignInFailsWithInvalidCode();
    }

    @Test
    void signIn_throwProviderUnavailable_whenGitHubRejectsClientCredentials() {
        GitHubTestData.expectTokenExchange(github, GitHubTestData.json(GitHubTestData.BAD_CLIENT_RESPONSE));

        assertSignInFailsAsUnavailable();
    }

    @Test
    void signIn_throwProviderUnavailable_whenExchangeAnswersWithoutTokenOrError() {
        GitHubTestData.expectTokenExchange(github, GitHubTestData.json("{}"));

        assertSignInFailsAsUnavailable();
    }

    @Test
    void signIn_throwInvalidCredential_whenExchangeAnswersWithClientError() {
        GitHubTestData.expectTokenExchange(github, MockRestResponseCreators.withBadRequest());

        assertSignInFailsWithInvalidCode();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unusableResponses")
    void signIn_throwProviderUnavailable_whenExchangeFails(final ResponseCreator response) {
        GitHubTestData.expectTokenExchange(github, response);

        assertSignInFailsAsUnavailable();
    }

    @Test
    void signIn_throwInvalidCredential_whenAccessTokenIsRejected() {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, MockRestResponseCreators.withUnauthorizedRequest());

        assertSignInFailsWithInvalidCode();
    }

    @Test
    void signIn_throwInvalidCredential_whenScopeForEmailsIsMissing() {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, GitHubTestData.json(GitHubTestData.accountJson()));
        GitHubTestData.expectEmails(github, MockRestResponseCreators.withResourceNotFound());

        assertSignInFailsWithInvalidCode();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unusableResponses")
    void signIn_throwProviderUnavailable_whenReadingAccountFails(final ResponseCreator response) {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, response);

        assertSignInFailsAsUnavailable();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unusableResponses")
    void signIn_throwProviderUnavailable_whenReadingEmailsFails(final ResponseCreator response) {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, GitHubTestData.json(GitHubTestData.accountJson()));
        GitHubTestData.expectEmails(github, response);

        assertSignInFailsAsUnavailable();
    }

    @Test
    void signIn_throwProviderUnavailable_whenAccountHasNoId() {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, GitHubTestData.json("{}"));
        GitHubTestData.expectEmails(github, GitHubTestData.json(GitHubTestData.emailsJson(true)));

        assertSignInFailsAsUnavailable();
    }

    @Test
    void signIn_returnTokensOfResolvedUser_whenCodeIsValid() {
        final var pippin = UserTestData.createTestUserPippin();
        final var tokens = new AuthTokensDto("access", "refresh", 900);
        GitHubTestData.expectSignIn(github, true);
        when(userIdentityService.resolveUser(any())).thenReturn(pippin.setId(7L));
        when(refreshTokenService.issueTokens(any())).thenReturn(tokens);

        final var result = service.signIn(GitHubTestData.createValidRequest());

        assertThat(result).isEqualTo(tokens);
        github.verify();
        assertThat(capturedProfile()).isEqualTo(new ProviderProfile(
                IdentityProvider.GITHUB,
                UserTestData.PROVIDER_USER_ID,
                pippin.getEmail(),
                EmailStatus.VERIFIED,
                pippin.getUserName(),
                pippin.getFirstName(),
                pippin.getLastName()));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("emails")
    void signIn_resolveUserWithPrimaryEmail(final String emails, final String email, final EmailStatus status) {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, GitHubTestData.json(GitHubTestData.accountJson()));
        GitHubTestData.expectEmails(github, GitHubTestData.json(emails));
        when(userIdentityService.resolveUser(any())).thenReturn(UserTestData.createTestUserPippin().setId(7L));

        service.signIn(GitHubTestData.createValidRequest());

        final var profile = capturedProfile();
        assertThat(profile.email()).isEqualTo(email);
        assertThat(profile.emailStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", delimiter = '|', value = {
            "null                 | null     | null",
            "'\"\"'               | null     | null",
            "'\"  \"'             | null     | null",
            "'\"Pippin\"'         | Pippin   | null",
            "'\"  Peregrin   Tuk \"' | Peregrin | Tuk",
            "'\"Jan van der Berg\"' | Jan    | van der Berg"
    })
    void signIn_splitNameAtFirstSpace(final String nameJson, final String firstName, final String lastName) {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, GitHubTestData.json(GitHubTestData.accountJson(String.valueOf(nameJson))));
        GitHubTestData.expectEmails(github, GitHubTestData.json(GitHubTestData.emailsJson(true)));
        when(userIdentityService.resolveUser(any())).thenReturn(UserTestData.createTestUserPippin().setId(7L));

        service.signIn(GitHubTestData.createValidRequest());

        final var profile = capturedProfile();
        assertThat(profile.firstName()).isEqualTo(firstName);
        assertThat(profile.lastName()).isEqualTo(lastName);
    }

    private ProviderProfile capturedProfile() {
        final var profile = ArgumentCaptor.forClass(ProviderProfile.class);
        verify(userIdentityService).resolveUser(profile.capture());
        return profile.getValue();
    }

    private void assertSignInFailsWithInvalidCode() {
        final var request = GitHubTestData.createValidRequest();

        assertThatThrownBy(() -> service.signIn(request))
                .isInstanceOf(InvalidProviderCredentialException.class)
                .hasMessage(GitHubTestData.INVALID_CODE);
        github.verify();
        verifyNoInteractions(userIdentityService, refreshTokenService);
    }

    private void assertSignInFailsAsUnavailable() {
        final var request = GitHubTestData.createValidRequest();

        assertThatThrownBy(() -> service.signIn(request))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasMessage(GitHubTestData.GITHUB_UNAVAILABLE)
                .cause()
                .isInstanceOf(RestClientException.class);
        github.verify();
        verifyNoInteractions(userIdentityService, refreshTokenService);
    }

    private GitHubSignInService createService(final String clientId,
                                              final String clientSecret,
                                              final RestClient.Builder builder) {
        return new GitHubSignInService(clientId, clientSecret, builder, userIdentityService, refreshTokenService);
    }

}
