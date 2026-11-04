package pl.prawko.prawko_server.service.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.prawko.prawko_server.config.AuthenticatedUser;
import pl.prawko.prawko_server.dto.AuthTokensDto;
import pl.prawko.prawko_server.dto.GitHubSignInRequest;
import pl.prawko.prawko_server.exception.InvalidProviderCredentialException;
import pl.prawko.prawko_server.exception.ProviderNotConfiguredException;
import pl.prawko.prawko_server.exception.ProviderUnavailableException;
import pl.prawko.prawko_server.model.EmailStatus;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.model.ProviderProfile;
import pl.prawko.prawko_server.service.IGitHubSignInService;
import pl.prawko.prawko_server.service.IRefreshTokenService;
import pl.prawko.prawko_server.service.IUserIdentityService;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link IGitHubSignInService} reading the account from GitHub's API.
 * <p>
 * GitHub has no ID tokens, so the authorization code is exchanged for an access token using {@code github.client-id}
 * and {@code github.client-secret}. Without both, GitHub sign-in is disabled. The access token is only used to read the
 * account's ID, login, name and emails, and is then discarded.
 * <p>
 * GitHub doesn't host mailboxes, it only verifies an address when it's added, so its emails are never
 * {@link EmailStatus#AUTHORITATIVE}.
 * <p>
 * Not transactional, so a conflict in {@link IUserIdentityService#resolveUser} is retried in a new transaction, and no
 * database connection is held during the requests to GitHub.
 */
@Service
public class GitHubSignInService implements IGitHubSignInService {

    private static final Logger log = LoggerFactory.getLogger(GitHubSignInService.class);
    private static final String TOKEN_URI = "https://github.com/login/oauth/access_token";
    private static final String USER_URI = "https://api.github.com/user";
    private static final String EMAILS_URI = "https://api.github.com/user/emails";
    private static final String API_VERSION_HEADER = "X-GitHub-Api-Version";
    private static final String API_VERSION = "2022-11-28";
    private static final MediaType API_MEDIA_TYPE = MediaType.parseMediaType("application/vnd.github+json");
    private static final String INCORRECT_CLIENT_CREDENTIALS = "incorrect_client_credentials";
    private static final String UNVERIFIED_USER_EMAIL = "unverified_user_email";
    private static final String RATE_LIMIT_REMAINING_HEADER = "x-ratelimit-remaining";
    private static final String INVALID_CODE = "GitHub authorization code is invalid or expired.";
    private static final String UNAVAILABLE = "Sign-in with GitHub is unavailable, try again later.";

    private final boolean configured;
    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;
    private final IUserIdentityService userIdentityService;
    private final IRefreshTokenService refreshTokenService;

    public GitHubSignInService(@Value("${github.client-id}") final String clientId,
                               @Value("${github.client-secret}") final String clientSecret,
                               final RestClient.Builder restClientBuilder,
                               final IUserIdentityService userIdentityService,
                               final IRefreshTokenService refreshTokenService) {
        this.configured = StringUtils.isNoneBlank(clientId, clientSecret);
        if (!configured) {
            log.info("GitHub client ID or secret not configured, sign-in with GitHub is disabled.");
        }
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restClient = restClientBuilder.build();
        this.userIdentityService = userIdentityService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public AuthTokensDto signIn(final GitHubSignInRequest request) {
        if (!configured) {
            throw new ProviderNotConfiguredException("Sign-in with GitHub is not available.");
        }
        final var user = userIdentityService.resolveUser(fetchProfile(request));
        return refreshTokenService.issueTokens(AuthenticatedUser.of(user));
    }

    /**
     * Logs only the response status of a rejected request, as its message contains the response body.
     */
    private ProviderProfile fetchProfile(final GitHubSignInRequest request) {
        try {
            final var accessToken = exchangeCode(request);
            final var account = fetch(USER_URI, accessToken, new ParameterizedTypeReference<GitHubAccount>() {
            });
            final var emails = fetch(EMAILS_URI, accessToken, new ParameterizedTypeReference<List<GitHubEmail>>() {
            });
            return toProfile(account, emails);
        } catch (final HttpClientErrorException exception) {
            if (isRateLimited(exception)) {
                log.warn("GitHub rate limited a sign-in request with status {}.", exception.getStatusCode());
                throw new ProviderUnavailableException(UNAVAILABLE, exception);
            }
            log.warn("GitHub rejected a sign-in request with status {}.", exception.getStatusCode());
            throw new InvalidProviderCredentialException(INVALID_CODE);
        } catch (final RestClientException exception) {
            log.error("GitHub account couldn't be read.", exception);
            throw new ProviderUnavailableException(UNAVAILABLE, exception);
        }
    }

    /**
     * GitHub answers a rate limit with {@code 429}, or with {@code 403} and a header saying so. Any other {@code 403}
     * is a rejected request.
     */
    private static boolean isRateLimited(final HttpClientErrorException exception) {
        final var status = exception.getStatusCode();
        if (status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
            return true;
        }
        return status.isSameCodeAs(HttpStatus.FORBIDDEN)
                && Optional.ofNullable(exception.getResponseHeaders())
                .filter(headers -> headers.containsHeaderValue(RATE_LIMIT_REMAINING_HEADER, "0")
                        || headers.containsHeader(HttpHeaders.RETRY_AFTER))
                .isPresent();
    }

    /**
     * GitHub answers a failed exchange with {@code 200} and an error code. Only rejected client credentials aren't the
     * client's fault. GitHub can also refuse an account with an unverified primary email here, before its emails can be
     * read.
     */
    private String exchangeCode(final GitHubSignInRequest request) {
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", request.code());
        form.add("code_verifier", request.codeVerifier());
        form.add("redirect_uri", request.redirectUri());
        final var response = restClient.post()
                .uri(TOKEN_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || (response.accessToken() == null && response.error() == null)) {
            throw new RestClientException("GitHub answered the code exchange without an access token or error.");
        }
        if (response.accessToken() != null) {
            return response.accessToken();
        }
        if (INCORRECT_CLIENT_CREDENTIALS.equals(response.error())) {
            throw new RestClientException("GitHub rejected the configured client ID or secret.");
        }
        if (UNVERIFIED_USER_EMAIL.equals(response.error())) {
            throw new DisabledException("Email address is not verified by GitHub.");
        }
        log.warn("GitHub rejected an authorization code: {}.", response.error());
        throw new InvalidProviderCredentialException(INVALID_CODE);
    }

    private <T> T fetch(final String uri, final String accessToken, final ParameterizedTypeReference<T> type) {
        final var body = restClient.get()
                .uri(uri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .accept(API_MEDIA_TYPE)
                .header(API_VERSION_HEADER, API_VERSION)
                .retrieve()
                .body(type);
        if (body == null) {
            throw new RestClientException("GitHub answered without a body: " + uri);
        }
        return body;
    }

    /**
     * Maps the account, suggesting its login as username. The login can be renamed, so the ID identifies the account.
     * The name is a single field at GitHub: its first word becomes the first name, the rest the last name.
     */
    private static ProviderProfile toProfile(final GitHubAccount account, final List<GitHubEmail> emails) {
        if (account.id() == null) {
            throw new RestClientException("GitHub answered with an account without an ID.");
        }
        final var primary = emails.stream()
                .filter(GitHubEmail::primary)
                .findFirst();
        final var name = StringUtils.normalizeSpace(account.name());
        return new ProviderProfile(
                IdentityProvider.GITHUB,
                account.id().toString(),
                primary.map(GitHubEmail::email)
                        .orElse(null),
                primary.filter(GitHubEmail::verified)
                        .map(email -> EmailStatus.VERIFIED)
                        .orElse(EmailStatus.UNVERIFIED),
                account.login(),
                StringUtils.trimToNull(StringUtils.substringBefore(name, " ")),
                StringUtils.trimToNull(StringUtils.substringAfter(name, " ")));
    }

    private record TokenResponse(@JsonProperty("access_token") @Nullable String accessToken, @Nullable String error) {
    }

    private record GitHubAccount(@Nullable Long id, @Nullable String login, @Nullable String name) {
    }

    private record GitHubEmail(String email, boolean primary, boolean verified) {
    }

}
