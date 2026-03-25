package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.QuestionDto;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.repository.QuestionRepository;
import pl.prawko.prawko_server.test_data.MultiPartFactory;
import pl.prawko.prawko_server.test_data.QuestionTestData;

import java.util.Collections;
import java.util.List;

@IntegrationTest
public class QuestionControllerTest {

    @Autowired
    private QuestionRepository repository;

    @LocalServerPort
    private int port;

    private RestTestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = TestUtils.createRestTestClient(port, ApiConstants.QUESTIONS_BASE_URL);
    }

    @Nested
    class AddQuestions {

        @Test
        void returnCreated_whenSuccess() {
            final var multipart = MultiPartFactory.fromClasspath("test_question.csv");

            restClient.post()
                    .headers(TestUtils::authAdmin)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipart)
                    .exchange()
                    .expectStatus().isCreated();
        }

        @Test
        void returnBadRequest_whenFileIsMissing() {
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
        void returnUnsupportedMediaType_whenFileFormatIsWrong() {
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

        @Test
        void returnForbidden_whenNotAdmin() {
            restClient.post()
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.post()
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

    }

    @Nested
    class GetQuestionById {

        @Test
        void returnQuestion_whenFound() {
            repository.save(QuestionTestData.createQuestion(QuestionType.SPECIAL));
            final var expected = QuestionTestData.createQuestionDto(QuestionType.SPECIAL);

            restClient.get()
                    .uri(ApiConstants.BY_ID, expected.id())
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(QuestionDto.class).isEqualTo(expected);
        }

        @Test
        void returnNotFound_whenNotFound() {
            final var nonExistentId = 666L;
            final var expected = "Question with id '" + nonExistentId + "' not found.";

            restClient.get()
                    .uri(ApiConstants.BY_ID, nonExistentId)
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(String.class).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, 0L})
        void returnBadRequest_whenIdIsNotPositive(long invalidId) {
            final var expectedMessage = "ID must be greater than 0.";

            restClient.get()
                    .uri(ApiConstants.BY_ID, invalidId)
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody(String.class).isEqualTo(expectedMessage);
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.get()
                    .uri(ApiConstants.BY_ID, 1L)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

    }

    @Nested
    class GetAllQuestions {

        @Test
        void returnQuestions_whenAnyExists() {
            repository.save(QuestionTestData.createQuestion(QuestionType.SPECIAL));
            final var expected = List.of(QuestionTestData.createQuestionDto(QuestionType.SPECIAL));

            restClient.get()
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<List<QuestionDto>>() {
                    }).isEqualTo(expected);
        }

        @Test
        void returnEmptyList_whenNoQuestionsExist() {
            restClient.get()
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<List<QuestionDto>>() {
                    })
                    .isEqualTo(Collections.emptyList());
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.get()
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        void returnForbidden_whenNotAdmin() {
            restClient.post()
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isForbidden();
        }

    }

}