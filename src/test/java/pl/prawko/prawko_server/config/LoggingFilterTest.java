package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import pl.prawko.prawko_server.constants.ApiConstants;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class LoggingFilterTest {

    @LocalServerPort
    private int port;

    @Test
    void logAuthenticatedUserName_whenRequestIsAuthenticated(final CapturedOutput output) {
        TestUtils.createRestTestClient(port, ApiConstants.USERS_BASE_URL)
                .get()
                .headers(TestUtils::authAdmin)
                .exchange()
                .expectStatus().isOk();

        assertThat(output.getOut())
                .contains("Request: method=GET uri=/users client=127.0.0.1 user=gimli")
                .contains("Response: method=GET uri=/users status=200");
    }

    @Test
    void logAnonymous_whenRequestIsNotAuthenticated(final CapturedOutput output) {
        TestUtils.createRestTestClient(port, ApiConstants.USERS_BASE_URL)
                .get()
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(output.getOut()).contains("Request: method=GET uri=/users client=127.0.0.1 user=anonymous");
    }

}
