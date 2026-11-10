package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.prawko.prawko_server.exception.InvalidProviderCredentialException;
import pl.prawko.prawko_server.exception.ProviderEmailNotVerifiedException;
import pl.prawko.prawko_server.exception.ProviderNotConfiguredException;
import pl.prawko.prawko_server.exception.ProviderUnavailableException;
import pl.prawko.prawko_server.model.EmailStatus;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.model.ProviderProfile;
import pl.prawko.prawko_server.service.implementation.GitHubProfileService;
import pl.prawko.prawko_server.testdata.GitHubTestData;
import pl.prawko.prawko_server.testdata.UserTestData;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubProfileServiceTest {

    private static final String RATE_LIMIT_REMAINING = "x-ratelimit-remaining";

    private MockRestServiceServer github;
    private GitHubProfileService service;

    private static Stream<Arguments> unusableResponses() {
        return Stream.of(
                Arguments.of(Named.of("server error", MockRestResponseCreators.withServerError())),
                Arguments.of(Named.of("bad gateway", MockRestResponseCreators.withStatus(HttpStatus.BAD_GATEWAY))),
                Arguments.of(Named.of("too many requests",
                        MockRestResponseCreators.withStatus(HttpStatus.TOO_MANY_REQUESTS))),
                Arguments.of(Named.of("forbidden, no requests remaining",
                        MockRestResponseCreators.withStatus(HttpStatus.FORBIDDEN)
                                .header(RATE_LIMIT_REMAINING, "0"))),
                Arguments.of(Named.of("forbidden, retry later",
                        MockRestResponseCreators.withStatus(HttpStatus.FORBIDDEN)
                                .header(HttpHeaders.RETRY_AFTER, "60"))),
                Arguments.of(Named.of("connection failure",
                        MockRestResponseCreators.withException(new IOException("connection refused")))),
                Arguments.of(Named.of("malformed body", GitHubTestData.json("{not json"))),
                Arguments.of(Named.of("empty body", MockRestResponseCreators.withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)))
        );
    }

    private static Stream<Arguments> forbiddenResponses() {
        return Stream.of(
                Arguments.of(Named.of("no rate limit headers",
                        MockRestResponseCreators.withStatus(HttpStatus.FORBIDDEN))),
                Arguments.of(Named.of("requests remaining",
                        MockRestResponseCreators.withStatus(HttpStatus.FORBIDDEN)
                                .header(RATE_LIMIT_REMAINING, "59")))
        );
    }

    private static Stream<Arguments> emails() {
        final var email = UserTestData.createTestUserPippin().getEmail();
        return Stream.of(
                Arguments.of(Named.of("primary verified", GitHubTestData.emailsJson()),
                        email, EmailStatus.VERIFIED),
                Arguments.of(Named.of("primary not verified", GitHubTestData.unverifiedEmailsJson()),
                        email, EmailStatus.UNVERIFIED),
                Arguments.of(Named.of("no primary", GitHubTestData.noPrimaryEmailsJson()),
                        null, EmailStatus.UNVERIFIED),
                Arguments.of(Named.of("no emails", "[]"), null, EmailStatus.UNVERIFIED)
        );
    }

    @BeforeEach
    void setUp() {
        final var builder = RestClient.builder();
        github = MockRestServiceServer.bindTo(builder)
                .build();
        service = new GitHubProfileService(GitHubTestData.CLIENT_ID, GitHubTestData.CLIENT_SECRET, builder);
    }

    @ParameterizedTest
    @CsvSource(value = {"'', secret", "id, ''", "'', ''"})
    void fetchProfile_throwProviderNotConfigured_whenClientCredentialsAreMissing(final String clientId,
                                                                                 final String clientSecret) {
        final var builder = RestClient.builder();
        final var server = MockRestServiceServer.bindTo(builder)
                .build();
        final var request = GitHubTestData.createValidRequest();
        final var disabled = new GitHubProfileService(clientId, clientSecret, builder);

        assertThatThrownBy(() -> disabled.fetchProfile(request))
                .isInstanceOf(ProviderNotConfiguredException.class)
                .hasMessage(GitHubTestData.GITHUB_NOT_AVAILABLE);
        server.verify();
    }

    @Test
    void fetchProfile_throwInvalidCredential_whenGitHubRejectsCode() {
        GitHubTestData.expectTokenExchange(github, GitHubTestData.json(GitHubTestData.BAD_CODE_RESPONSE));

        assertFetchFailsWithInvalidCode();
    }

    @Test
    void fetchProfile_throwProviderUnavailable_whenGitHubRejectsClientCredentials() {
        GitHubTestData.expectTokenExchange(github, GitHubTestData.json(GitHubTestData.BAD_CLIENT_RESPONSE));

        assertFetchFailsAsUnavailable();
    }

    @Test
    void fetchProfile_throwNotVerified_whenExchangeRefusesUnverifiedEmail() {
        GitHubTestData.expectTokenExchange(github, GitHubTestData.json(GitHubTestData.UNVERIFIED_EMAIL_RESPONSE));
        final var request = GitHubTestData.createValidRequest();

        assertThatThrownBy(() -> service.fetchProfile(request))
                .isInstanceOf(ProviderEmailNotVerifiedException.class)
                .hasMessage(GitHubTestData.EMAIL_NOT_VERIFIED);
        github.verify();
    }

    @Test
    void fetchProfile_throwProviderUnavailable_whenExchangeAnswersWithoutTokenOrError() {
        GitHubTestData.expectTokenExchange(github, GitHubTestData.json("{}"));

        assertFetchFailsAsUnavailable();
    }

    @Test
    void fetchProfile_throwInvalidCredential_whenExchangeAnswersWithClientError() {
        GitHubTestData.expectTokenExchange(github, MockRestResponseCreators.withBadRequest());

        assertFetchFailsWithInvalidCode();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unusableResponses")
    void fetchProfile_throwProviderUnavailable_whenExchangeFails(final ResponseCreator response) {
        GitHubTestData.expectTokenExchange(github, response);

        assertFetchFailsAsUnavailable();
    }

    @Test
    void fetchProfile_throwInvalidCredential_whenAccessTokenIsRejected() {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, MockRestResponseCreators.withUnauthorizedRequest());

        assertFetchFailsWithInvalidCode();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("forbiddenResponses")
    void fetchProfile_throwInvalidCredential_whenForbiddenWithoutRateLimit(final ResponseCreator response) {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, response);

        assertFetchFailsWithInvalidCode();
    }

    @Test
    void fetchProfile_throwInvalidCredential_whenScopeForEmailsIsMissing() {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, GitHubTestData.json(GitHubTestData.accountJson()));
        GitHubTestData.expectEmails(github, MockRestResponseCreators.withResourceNotFound());

        assertFetchFailsWithInvalidCode();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unusableResponses")
    void fetchProfile_throwProviderUnavailable_whenReadingAccountFails(final ResponseCreator response) {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, response);

        assertFetchFailsAsUnavailable();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unusableResponses")
    void fetchProfile_throwProviderUnavailable_whenReadingEmailsFails(final ResponseCreator response) {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, GitHubTestData.json(GitHubTestData.accountJson()));
        GitHubTestData.expectEmails(github, response);

        assertFetchFailsAsUnavailable();
    }

    @Test
    void fetchProfile_throwProviderUnavailable_whenAccountHasNoId() {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, GitHubTestData.json("{}"));
        GitHubTestData.expectEmails(github, GitHubTestData.json(GitHubTestData.emailsJson()));

        assertFetchFailsAsUnavailable();
    }

    @Test
    void fetchProfile_returnProfile_whenCodeIsValid() {
        final var pippin = UserTestData.createTestUserPippin();
        GitHubTestData.expectSignIn(github);

        final var result = service.fetchProfile(GitHubTestData.createValidRequest());

        github.verify();
        assertThat(result).isEqualTo(new ProviderProfile(
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
    void fetchProfile_returnPrimaryEmail_whenCodeIsValid(final String emails,
                                                         final String email,
                                                         final EmailStatus status) {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, GitHubTestData.json(GitHubTestData.accountJson()));
        GitHubTestData.expectEmails(github, GitHubTestData.json(emails));

        final var result = service.fetchProfile(GitHubTestData.createValidRequest());

        assertThat(result.email()).isEqualTo(email);
        assertThat(result.emailStatus()).isEqualTo(status);
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
    void fetchProfile_splitNameAtFirstSpace_whenCodeIsValid(final String nameJson,
                                                            final String firstName,
                                                            final String lastName) {
        GitHubTestData.expectTokenExchange(github);
        GitHubTestData.expectAccount(github, GitHubTestData.json(GitHubTestData.accountJson(String.valueOf(nameJson))));
        GitHubTestData.expectEmails(github, GitHubTestData.json(GitHubTestData.emailsJson()));

        final var result = service.fetchProfile(GitHubTestData.createValidRequest());

        assertThat(result.firstName()).isEqualTo(firstName);
        assertThat(result.lastName()).isEqualTo(lastName);
    }

    private void assertFetchFailsWithInvalidCode() {
        final var request = GitHubTestData.createValidRequest();

        assertThatThrownBy(() -> service.fetchProfile(request))
                .isInstanceOf(InvalidProviderCredentialException.class)
                .hasMessage(GitHubTestData.INVALID_CODE);
        github.verify();
    }

    private void assertFetchFailsAsUnavailable() {
        final var request = GitHubTestData.createValidRequest();

        assertThatThrownBy(() -> service.fetchProfile(request))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasMessage(GitHubTestData.GITHUB_UNAVAILABLE)
                .cause()
                .isInstanceOf(RestClientException.class);
        github.verify();
    }

}
