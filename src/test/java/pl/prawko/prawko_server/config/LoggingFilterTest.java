package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.test_data.UserTestData;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class LoggingFilterTest {

    @Autowired
    private UserRepository userRepository;

    @LocalServerPort
    private int port;

    private RestTestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = TestUtils.createRestTestClient(port, ApiConstants.USERS_BASE_URL);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void doFilterInternal_logUserIdOnly_whenRequestIsAuthenticated(final CapturedOutput output) {
        final var tester = userRepository.save(UserTestData.createTestUserPippin());

        restClient.get()
                .uri(ApiConstants.ME)
                .headers(TestUtils::authUser)
                .exchange()
                .expectStatus().isOk();

        assertThat(output)
                .contains("status=200", "user=" + tester.getId(), "User with id '" + tester.getId() + "' logged successfully.")
                .doesNotContain(tester.getUserName(), tester.getEmail());
    }

    @Test
    void doFilterInternal_logAnonymous_whenRequestIsNotAuthenticated(final CapturedOutput output) {
        restClient.get()
                .uri(ApiConstants.ME)
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(output).contains("status=401", "user=anonymous");
    }

}
