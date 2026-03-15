package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import pl.prawko.prawko_server.test_data.TestDataFactory;

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

    private final TestDataFactory testDataFactory = new TestDataFactory();

    @BeforeEach
    void setUp() {
        restClient = TestUtils.createRestTestClient(port, ApiConstants.EXAMS_BASE_URL);
    }

    @Test
    void createExam_returnsCreated_whenRequestIsValid() {
        final var tester = userRepository.save(testDataFactory.createTestUserPippin());
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
    void createExam_returnsBadRequest_whenRequestIsInvalid() {
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
    void getExam_returnsNotFound_whenExamIsNotFound() {
        final var nonExistingId = 666L;
        final var expected = "Exam with '" + nonExistingId + "' not found.";

        restClient.get()
                .uri(ApiConstants.BY_ID, nonExistingId)
                .headers(TestUtils::authUser)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).isEqualTo(expected);
    }

    @Test
    void getExam_returnsExam_whenExamIsFound() {
        final var tester = userRepository.save(testDataFactory.createTestUserPippin());
        final var exam = examRepository.save(testDataFactory.createExam(tester));
        final var expected = testDataFactory.createExamDto(exam);

        restClient.get()
                .uri(ApiConstants.BY_ID, exam.getId())
                .headers(TestUtils::authUser)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExamDto.class).isEqualTo(expected);
    }

}
