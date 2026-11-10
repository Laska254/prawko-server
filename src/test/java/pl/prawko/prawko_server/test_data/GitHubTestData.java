package pl.prawko.prawko_server.test_data;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.util.LinkedMultiValueMap;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.dto.request.GitHubSignInRequest;
import pl.prawko.prawko_server.dto.request.LinkGitHubRequest;

/**
 * Requests and answers of GitHub's OAuth flow and API, stubbed on a {@link MockRestServiceServer}.
 */
public class GitHubTestData {

    public static final String CLIENT_ID = "github-client-id";
    public static final String CLIENT_SECRET = "github-client-secret";
    public static final String ACCESS_TOKEN = "github-access-token";
    public static final String INVALID_CODE = "GitHub authorization code is invalid or expired.";
    public static final String GITHUB_NOT_AVAILABLE = "Sign-in with GitHub is not available.";
    public static final String GITHUB_UNAVAILABLE = "Sign-in with GitHub is unavailable, try again later.";
    public static final String EMAIL_NOT_VERIFIED = "Email address is not verified by GitHub.";
    public static final String EMAIL_NOT_HOSTED = "User with this email already exists, and GitHub doesn't host the "
            + "email, so the account can't be linked to it. Sign in as that user and link GitHub from the "
            + "account settings.";
    public static final String ALREADY_LINKED = "User is already linked to a GitHub account.";
    public static final String NOT_LINKED = "User has no GitHub account linked.";
    public static final String BAD_CODE_RESPONSE = """
            {"error": "bad_verification_code", "error_description": "The code passed is incorrect or expired."}""";
    public static final String BAD_CLIENT_RESPONSE = """
            {"error": "incorrect_client_credentials", \
            "error_description": "The client_id or client_secret is wrong."}""";
    public static final String UNVERIFIED_EMAIL_RESPONSE = """
            {"error": "unverified_user_email", "error_description": "The user must have a verified primary email."}""";

    private static final String TOKEN_URI = "https://github.com/login/oauth/access_token";
    private static final String USER_URI = "https://api.github.com/user";
    private static final String EMAILS_URI = "https://api.github.com/user/emails";
    private static final String CODE = "github-code";
    private static final String CODE_VERIFIER = "github-code-verifier";
    private static final String REDIRECT_URI = "prawko://auth/github";
    private static final String OTHER_EMAIL = "other@shire.me";
    private static final String TOKEN_RESPONSE = """
            {"access_token": "%s", "scope": "user:email", "token_type": "bearer"}""".formatted(ACCESS_TOKEN);
    private static final String EMAILS_TEMPLATE = """
            [{"email": "%s", "primary": false, "verified": true, "visibility": null},
             {"email": "%s", "primary": true, "verified": %s, "visibility": "private"}]""";

    private GitHubTestData() {
    }

    public static GitHubSignInRequest createValidRequest() {
        return new GitHubSignInRequest(CODE, CODE_VERIFIER, REDIRECT_URI);
    }

    public static LinkGitHubRequest createLinkRequest(@Nullable final String currentPassword) {
        return new LinkGitHubRequest(CODE, CODE_VERIFIER, REDIRECT_URI, currentPassword);
    }

    public static LinkGitHubRequest createValidLinkRequest() {
        return createLinkRequest(TestUtils.USER_PASSWORD);
    }

    /**
     * Account of Pippin with the given name, which is JSON: quoted or {@code null}.
     */
    public static String accountJson(final String nameJson) {
        return """
                {"id": %s, "login": "%s", "name": %s, "site_admin": false}"""
                .formatted(UserTestData.PROVIDER_USER_ID, UserTestData.createTestUserPippin().getUserName(), nameJson);
    }

    public static String accountJson() {
        final var pippin = UserTestData.createTestUserPippin();
        return accountJson('"' + pippin.getFirstName() + ' ' + pippin.getLastName() + '"');
    }

    /**
     * Pippin's verified primary email, listed after a verified non-primary one.
     */
    public static String emailsJson() {
        return EMAILS_TEMPLATE.formatted(OTHER_EMAIL, UserTestData.createTestUserPippin().getEmail(), true);
    }

    /**
     * Pippin's unverified primary email, listed after a verified non-primary one.
     */
    public static String unverifiedEmailsJson() {
        return EMAILS_TEMPLATE.formatted(OTHER_EMAIL, UserTestData.createTestUserPippin().getEmail(), false);
    }

    public static String noPrimaryEmailsJson() {
        return """
                [{"email": "%s", "primary": false, "verified": true}]""".formatted(OTHER_EMAIL);
    }

    public static void expectTokenExchange(final MockRestServiceServer github, final ResponseCreator response) {
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", CLIENT_ID);
        form.add("client_secret", CLIENT_SECRET);
        form.add("code", CODE);
        form.add("code_verifier", CODE_VERIFIER);
        form.add("redirect_uri", REDIRECT_URI);
        github.expect(MockRestRequestMatchers.requestTo(TOKEN_URI))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.content().formData(form))
                .andRespond(response);
    }

    public static void expectTokenExchange(final MockRestServiceServer github) {
        expectTokenExchange(github, json(TOKEN_RESPONSE));
    }

    public static void expectAccount(final MockRestServiceServer github, final ResponseCreator response) {
        expectApiRequest(github, USER_URI)
                .andRespond(response);
    }

    public static void expectEmails(final MockRestServiceServer github, final ResponseCreator response) {
        expectApiRequest(github, EMAILS_URI)
                .andRespond(response);
    }

    /**
     * Stubs a sign-in with a valid code of Pippin's GitHub account.
     */
    public static void expectSignIn(final MockRestServiceServer github) {
        expectSignIn(github, emailsJson());
    }

    /**
     * Stubs a sign-in with a valid code of Pippin's GitHub account, whose primary email isn't verified.
     */
    public static void expectSignInWithUnverifiedEmail(final MockRestServiceServer github) {
        expectSignIn(github, unverifiedEmailsJson());
    }

    public static ResponseCreator json(final String body) {
        return MockRestResponseCreators.withSuccess(body, MediaType.APPLICATION_JSON);
    }

    private static void expectSignIn(final MockRestServiceServer github, final String emailsJson) {
        expectTokenExchange(github);
        expectAccount(github, json(accountJson()));
        expectEmails(github, json(emailsJson));
    }

    private static ResponseActions expectApiRequest(final MockRestServiceServer github, final String uri) {
        return github.expect(MockRestRequestMatchers.requestTo(uri))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN));
    }

}
