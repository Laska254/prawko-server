package pl.prawko.prawko_server.config;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.service.IAccessTokenService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestUtils {

    public static final String BASE_URL = "http://localhost:";

    public static final String USER_NAME = "pippin";
    public static final String USER_PASSWORD = "lembasy";
    public static final String ADMIN_NAME = "gimli";
    public static final String ADMIN_PASSWORD = "krasnoludka";
    public static final String UNVERIFIED_NAME = "frodo";
    public static final String UNVERIFIED_PASSWORD = "myprecious";

    public static final String ACCESS_DENIED = "Access denied.";
    public static final String INVALID_CREDENTIALS = "Invalid login or password.";
    public static final String VALIDATION_FAILED = "Validation for request failed.";
    public static final String BODY_MISSING = "Request body is missing.";
    public static final String ID_NOT_POSITIVE = "ID must be greater than 0.";
    public static final String INVALID_SORT = "Cannot sort by 'nonExisting'.";
    public static final String USERNAME_REQUIRED = "Username is required.";
    public static final String PASSWORD_REQUIRED = "Password is required.";
    public static final String SERVER_BUSY = "Server is busy, try again later.";

    public static final long ASYNC_TIMEOUT_MS = 5_000;

    public static final long PRINCIPAL_ID = 7L;

    // a fixed time in a zone far from UTC: the results must not depend on it
    public static final Clock CLOCK = Clock.fixed(Instant.now(), ZoneId.of("Pacific/Kiritimati"));

    public static void authUser(final HttpHeaders headers) {
        headers.setBasicAuth(USER_NAME, USER_PASSWORD);
    }

    public static void authAdmin(final HttpHeaders headers) {
        headers.setBasicAuth(ADMIN_NAME, ADMIN_PASSWORD);
    }

    public static void authUnverified(final HttpHeaders headers) {
        headers.setBasicAuth(UNVERIFIED_NAME, UNVERIFIED_PASSWORD);
    }

    public static AuthenticatedUser createUserPrincipal() {
        return new AuthenticatedUser(
                PRINCIPAL_ID, USER_NAME, USER_PASSWORD, true, AuthorityUtils.createAuthorityList(Role.USER.getAuthority()));
    }

    public static AuthenticatedUser createAdminPrincipal() {
        return new AuthenticatedUser(
                PRINCIPAL_ID, ADMIN_NAME, ADMIN_PASSWORD, true, AuthorityUtils.createAuthorityList(Role.ADMIN.getAuthority()));
    }

    public static JwtClaimsSet adminClaims(final Instant expiresAt) {
        return JwtClaimsSet.builder()
                .subject(String.valueOf(PRINCIPAL_ID))
                .claim(JwtConfig.USERNAME_CLAIM, ADMIN_NAME)
                .claim(JwtConfig.AUTHORITIES_CLAIM, List.of(Role.ADMIN.getAuthority()))
                .expiresAt(expiresAt)
                .build();
    }

    public static String issueAccessToken(final IAccessTokenService accessTokenService,
                                          final UserDetailsService userDetailsService,
                                          final String username) {
        final var principal = (AuthenticatedUser) userDetailsService.loadUserByUsername(username);
        return accessTokenService.issue(principal)
                .getTokenValue();
    }

    public static String invalidValueMessage(final String value, final String parameter) {
        return "Invalid value '" + value + "' for '" + parameter + "'.";
    }

    public static RestTestClient createRestTestClient(final int port, final String controllerBasePath) {
        return RestTestClient
                .bindToServer()
                .baseUrl(TestUtils.BASE_URL + port + controllerBasePath)
                .build();
    }

    public static CountDownLatch blockTaskExecutor(final ThreadPoolTaskExecutor taskExecutor) {
        final var release = new CountDownLatch(1);
        do {
            try {
                while (true) {
                    taskExecutor.execute(() -> awaitRelease(release));
                }
            } catch (final TaskRejectedException full) {
                Thread.onSpinWait();
            }
        } while (taskExecutor.getActiveCount() < taskExecutor.getMaxPoolSize()
                || taskExecutor.getQueueSize() < taskExecutor.getQueueCapacity());
        return release;
    }

    private static void awaitRelease(final CountDownLatch release) {
        try {
            release.await(ASYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

}
