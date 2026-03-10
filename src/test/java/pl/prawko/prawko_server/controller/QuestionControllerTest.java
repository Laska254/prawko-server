package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.test_data.MultiPartFactory;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class QuestionControllerTest {

    public static final String URL = "/questions";

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl(TestUtils.BASE_URL + port)
                .build();
    }

    @Test
    void addQuestions_throwBadRequest_whenFileIsMissing() {
        final var multipart = MultiPartFactory.empty();

        final var response = exchangeMultiPart(multipart);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addQuestions_throwUnsupportedMediaType_whenFileFormatIsWrong() {
        final var multipart = MultiPartFactory.withWrongFile();

        final var response = exchangeMultiPart(multipart);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void addQuestions_returnOk_whenSuccess() {
        final var multipart = MultiPartFactory.fromClasspath("test_question.csv");

        final var response = restClient.post()
                .uri(URL)
                .headers(TestUtils::authAdmin)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipart)
                .retrieve()
                .toEntity(ResponseEntity.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ResponseEntity<Void> exchangeMultiPart(final MultiValueMap<String, Object> multipart) {
        return restClient.post()
                .uri(URL)
                .headers(TestUtils::authAdmin)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipart)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                })
                .toBodilessEntity();
    }

}
