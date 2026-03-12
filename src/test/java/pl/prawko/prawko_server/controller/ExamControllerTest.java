package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
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
import static pl.prawko.prawko_server.config.TestUtils.BASE_URL;

@IntegrationTest
public class ExamControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    @LocalServerPort
    private int port;

    private RestClient restClient;

    private final TestDataFactory testDataFactory = new TestDataFactory();

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl(BASE_URL + port)
                .build();
    }

    @Test
    void createExam_returnsCreated_whenRequestIsValid() {
        final var tester = userRepository.save(testDataFactory.createTestUserPippin());
        final var dto = new CreateExamDto(tester.getId(), CategoryVariant.B);

        final var response = restClient.post()
                .uri(ApiConstants.EXAMS_BASE_URL)
                .headers(TestUtils::authUser)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
        final var createdExam = examRepository.findAll().getFirst();
        final var expectedLocation = ApiConstants.EXAMS_BASE_URL + "/" + createdExam.getId();
        final var location = response.getHeaders().getLocation().getPath();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(location).isEqualTo(expectedLocation);
    }

    @Test
    void createExam_returnsBadRequest_whenRequestIsInvalid() {
        final var dto = new CreateExamDto(null, null);
        final var expectedMessage = "Validation for request failed.";
        final var expectedDetails = Map.of(
                "userId", "userId is required",
                "categoryName", "category is required"
        );

        final var response = restClient.post()
                .uri(ApiConstants.EXAMS_BASE_URL)
                .headers(TestUtils::authUser)
                .body(dto)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(Map.class), res.getStatusCode()));

        final var body = (Map<String, Object>) response.getBody();
        final var message = body.get("message");
        final var details = (Map<String, String>) body.get("details");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(message).isEqualTo(expectedMessage);
        assertThat(details).containsAllEntriesOf(expectedDetails);
    }

    @Test
    void getExam_returnsExam_whenExamIsFound() {
        final var tester = userRepository.save(testDataFactory.createTestUserPippin());
        final var exam = examRepository.save(testDataFactory.createExam(tester));

        final var response = restClient.get()
                .uri(ApiConstants.EXAMS_BASE_URL + ApiConstants.BY_ID, exam.getId())
                .headers(TestUtils::authUser)
                .retrieve()
                .toEntity(ExamDto.class);
        final var body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.userId()).isEqualTo(exam.getUser().getId());
        assertThat(body.id()).isEqualTo(exam.getId());
        //TODO add createExamDto to factory and check if object is equals
    }

    @Test
    void getExam_returnsNotFound_whenExamIsNotFound() {
        final var notExistingId = 666L;
        final var response = restClient.get()
                .uri(ApiConstants.EXAMS_BASE_URL + ApiConstants.BY_ID, notExistingId)
                .headers(TestUtils::authUser)
                .exchange((req, res) ->
                        new ResponseEntity<>(res.bodyTo(String.class), res.getStatusCode()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotBlank();
    }

}
