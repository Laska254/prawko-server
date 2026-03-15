package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.CreateExamDto;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.model.CategoryVariant;
import pl.prawko.prawko_server.repository.ExamRepository;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.test_data.ExamTestData;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class ExamControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    @LocalServerPort
    private int port;

    private RestTestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = TestUtils.createRestTestClient(port, ApiConstants.EXAMS_BASE_URL);
    }

    @Nested
    class CreateExam {

        @Test
        void returnCreated_whenRequestIsValid() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var validDto = new CreateExamDto(tester.getId(), CategoryVariant.B);

            restClient.post()
                    .headers(TestUtils::authUser)
                    .body(validDto)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().value("Location", location -> {
                        final var createdExam = examRepository.findAll().getFirst();
                        final var expectedLocation = ApiConstants.EXAMS_BASE_URL + "/" + createdExam.getId();
                        assertThat(location).endsWith(expectedLocation);
                    })
                    .expectBody().isEmpty();
        }

        @Test
        void returnBadRequest_whenRequestIsInvalid() {
            final var invalidDto = new CreateExamDto(null, null);
            final var expected = Map.of(
                    "message", "Validation for request failed.",
                    "details", Map.of(
                            "userId", "userId is required",
                            "categoryName", "category is required"
                    )
            );

            restClient.post()
                    .headers(TestUtils::authUser)
                    .body(invalidDto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody(Map.class).isEqualTo(expected);
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.post()
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

    }

    @Nested
    class GetExamById {

        @Test
        void returnsExam_whenExamIsFound() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var exam = examRepository.save(ExamTestData.createExam(tester));
            final var expected = ExamTestData.createExamDto(exam);

            restClient.get()
                    .uri(ApiConstants.BY_ID, exam.getId())
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ExamDto.class).isEqualTo(expected);
        }

        @Test
        void returnsNotFound_whenExamIsNotFound() {
            final var nonExistingId = 666L;
            final var expected = "Exam with '" + nonExistingId + "' not found.";

            restClient.get()
                    .uri(ApiConstants.BY_ID, nonExistingId)
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
                    .uri(ApiConstants.BY_ID, 666L)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

    }

}
