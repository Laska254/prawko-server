package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.test_data.MultiPartFactory;

@IntegrationTest
public class QuestionControllerTest {

    @LocalServerPort
    private int port;

    private RestTestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = TestUtils.createRestTestClient(port, ApiConstants.QUESTIONS_BASE_URL);
    }

    @Test
    void addQuestions_returnOk_whenSuccess() {
        final var multipart = MultiPartFactory.fromClasspath("test_question.csv");

        restClient.post()
                .headers(TestUtils::authAdmin)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipart)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void addQuestions_throwBadRequest_whenFileIsMissing() {
        final var multipart = MultiPartFactory.empty();
        final var expected = "Required part 'file' is not present.";

        restClient.post()
                .headers(TestUtils::authAdmin)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipart)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo(expected);
    }

    @Test
    void addQuestions_throwUnsupportedMediaType_whenFileFormatIsWrong() {
        final var multipart = MultiPartFactory.withWrongFile();
        final var expected = "Invalid file format.";

        restClient.post()
                .headers(TestUtils::authAdmin)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipart)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .expectBody(String.class).isEqualTo(expected);
    }

}
