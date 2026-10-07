package pl.prawko.prawko_server.config;

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.RestTestClient;

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

}
