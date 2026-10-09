package pl.prawko.prawko_server.config;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestUtils {

    public static final String BASE_URL = "http://localhost:";

    public static final String USER_NAME = "pippin";
    public static final String USER_PASSWORD = "lembasy";
    public static final String ADMIN_NAME = "gimli";
    public static final String ADMIN_PASSWORD = "krasnoludka";

    public static final String ACCESS_DENIED = "Access denied.";
    public static final String INVALID_CREDENTIALS = "Invalid login or password.";
    public static final String VALIDATION_FAILED = "Validation for request failed.";
    public static final String BODY_MISSING = "Request body is missing.";
    public static final String ID_NOT_POSITIVE = "ID must be greater than 0.";
    public static final String INVALID_SORT = "Cannot sort by 'nonExisting'.";
    public static final String USERNAME_REQUIRED = "Username is required.";
    public static final String PASSWORD_REQUIRED = "Password is required.";
    public static final String SERVER_BUSY = "Server is busy, try again later.";

    public static final long MAIL_TIMEOUT_MS = 5_000;

    public static void authUser(final HttpHeaders headers) {
        headers.setBasicAuth(USER_NAME, USER_PASSWORD);
    }

    public static void authAdmin(final HttpHeaders headers) {
        headers.setBasicAuth(ADMIN_NAME, ADMIN_PASSWORD);
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
            release.await(MAIL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

}
