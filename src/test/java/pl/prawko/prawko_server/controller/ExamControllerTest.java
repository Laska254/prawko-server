package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.PageResponse;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.CreateExamDto;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.dto.ExamSummaryDto;
import pl.prawko.prawko_server.model.CategoryVariant;
import pl.prawko.prawko_server.repository.ExamRepository;
import pl.prawko.prawko_server.repository.QuestionRepository;
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

    @Autowired
    private QuestionRepository questionRepository;

    @LocalServerPort
    private int port;

    private RestTestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = TestUtils.createRestTestClient(port, ApiConstants.EXAMS_BASE_URL);
    }

    @AfterEach
    void tearDown() {
        examRepository.deleteAll();
        userRepository.deleteAll();
        questionRepository.deleteAll();
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
        void returnCreated_whenAdminCreatesExamForAnotherUser() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());

            restClient.post()
                    .headers(TestUtils::authAdmin)
                    .body(new CreateExamDto(tester.getId(), CategoryVariant.B))
                    .exchange()
                    .expectStatus().isCreated();
        }

        @Test
        void returnForbidden_whenCreatingExamForAnotherUser() {
            userRepository.save(UserTestData.createTestUserPippin());
            final var other = userRepository.save(UserTestData.createMerry());

            restClient.post()
                    .headers(TestUtils::authUser)
                    .body(new CreateExamDto(other.getId(), CategoryVariant.B))
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.ACCESS_DENIED);

            assertThat(examRepository.findAll()).isEmpty();
        }

        @Test
        void returnBadRequest_whenRequestIsInvalid() {
            final var invalidDto = new CreateExamDto(null, null);
            final var expected = Map.ofEntries(
                    Map.entry("message", TestUtils.VALIDATION_FAILED),
                    Map.entry("details", Map.ofEntries(
                            Map.entry("userId", "User ID is required."),
                            Map.entry("categoryName", "Category is required."))));

            restClient.post()
                    .headers(TestUtils::authUser)
                    .body(invalidDto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expected.get("message"))
                    .jsonPath("$.details").isEqualTo(expected.get("details"));
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
            final var exam = ExamTestData.createExam(tester);
            exam.setQuestions(questionRepository.saveAll(exam.getQuestions()));
            examRepository.save(exam);
            final var expected = ExamTestData.createExamDto(exam);

            restClient.get()
                    .uri(ApiConstants.BY_ID, exam.getId())
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ExamDto.class).isEqualTo(expected);
        }

        @Test
        void returnsExam_whenAdminRequestsExamOfAnotherUser() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var exam = examRepository.save(ExamTestData.createExamWithoutQuestions(tester));

            restClient.get()
                    .uri(ApiConstants.BY_ID, exam.getId())
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(exam.getId());
        }

        @Test
        void returnsForbidden_whenExamBelongsToAnotherUser() {
            userRepository.save(UserTestData.createTestUserPippin());
            final var other = userRepository.save(UserTestData.createMerry());
            final var exam = examRepository.save(ExamTestData.createExamWithoutQuestions(other));

            restClient.get()
                    .uri(ApiConstants.BY_ID, exam.getId())
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.ACCESS_DENIED);
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
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, 0L})
        void returnBadRequest_whenIdIsNotPositive(long invalidId) {
            final var expectedMessage = TestUtils.ID_NOT_POSITIVE;

            restClient.get()
                    .uri(ApiConstants.BY_ID, invalidId)
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(expectedMessage);
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.get()
                    .uri(ApiConstants.BY_ID, 666L)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

    }

    @Nested
    class GetUserExams {

        @Test
        void returnUserExams_fromNewest_whenUserHasExams() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var other = userRepository.save(UserTestData.createMerry());
            final var older = examRepository.save(ExamTestData.createExamWithoutQuestions(tester));
            final var newer = examRepository.save(ExamTestData.createExamWithoutQuestions(tester));
            examRepository.save(ExamTestData.createExamWithoutQuestions(other));

            final var result = restClient.get()
                    .uri(uri -> uri.queryParam("userId", tester.getId()).build())
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<PageResponse<ExamSummaryDto>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assertThat(result.content())
                    .extracting(ExamSummaryDto::id)
                    .containsExactly(newer.getId(), older.getId());
            assertThat(result.page().totalElements()).isEqualTo(2);
            assertThat(result.content())
                    .allSatisfy(summary -> {
                        assertThat(summary.category()).isEqualTo("B");
                        assertThat(summary.active()).isTrue();
                        assertThat(summary.score()).isZero();
                        assertThat(summary.created()).isNotNull();
                    });
        }

        @Test
        void returnRequestedPage_whenPageAndSizeAreGiven() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var oldest = examRepository.save(ExamTestData.createExamWithoutQuestions(tester));
            examRepository.save(ExamTestData.createExamWithoutQuestions(tester));
            examRepository.save(ExamTestData.createExamWithoutQuestions(tester));

            restClient.get()
                    .uri(uri -> uri
                            .queryParam("userId", tester.getId())
                            .queryParam("page", 1)
                            .queryParam("size", 2)
                            .build())
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content.length()").isEqualTo(1)
                    .jsonPath("$.content[0].id").isEqualTo(oldest.getId())
                    .jsonPath("$.page.number").isEqualTo(1)
                    .jsonPath("$.page.size").isEqualTo(2)
                    .jsonPath("$.page.totalElements").isEqualTo(3)
                    .jsonPath("$.page.totalPages").isEqualTo(2);
        }

        @Test
        void returnUserExams_inRequestedOrder_whenSortIsGiven() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var older = examRepository.save(ExamTestData.createExamWithoutQuestions(tester));
            final var newer = examRepository.save(ExamTestData.createExamWithoutQuestions(tester));

            restClient.get()
                    .uri(uri -> uri
                            .queryParam("userId", tester.getId())
                            .queryParam("sort", "id,asc")
                            .build())
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content[0].id").isEqualTo(older.getId())
                    .jsonPath("$.content[1].id").isEqualTo(newer.getId());
        }

        @Test
        void returnEmptyPage_whenUserHasNoExams() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());

            restClient.get()
                    .uri(uri -> uri.queryParam("userId", tester.getId()).build())
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content").isEmpty()
                    .jsonPath("$.page.totalElements").isEqualTo(0);
        }

        @Test
        void returnBadRequest_whenSortPropertyIsInvalid() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());

            restClient.get()
                    .uri(uri -> uri
                            .queryParam("userId", tester.getId())
                            .queryParam("sort", "nonExisting")
                            .build())
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.INVALID_SORT);
        }

        @Test
        void returnUserExams_whenAdminRequestsExamsOfAnotherUser() {
            final var tester = userRepository.save(UserTestData.createTestUserPippin());
            final var exam = examRepository.save(ExamTestData.createExamWithoutQuestions(tester));

            restClient.get()
                    .uri(uri -> uri.queryParam("userId", tester.getId()).build())
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content[0].id").isEqualTo(exam.getId());
        }

        @Test
        void returnForbidden_whenRequestingExamsOfAnotherUser() {
            userRepository.save(UserTestData.createTestUserPippin());
            final var other = userRepository.save(UserTestData.createMerry());

            restClient.get()
                    .uri(uri -> uri.queryParam("userId", other.getId()).build())
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.ACCESS_DENIED);
        }

        @Test
        void returnNotFound_whenUserDoesNotExist() {
            final var nonExistentId = 666L;

            restClient.get()
                    .uri(uri -> uri.queryParam("userId", nonExistentId).build())
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(UserTestData.userNotFoundMessage(nonExistentId));
        }

        @ParameterizedTest
        @ValueSource(longs = {-1L, 0L})
        void returnBadRequest_whenUserIdIsNotPositive(long invalidId) {
            restClient.get()
                    .uri(uri -> uri.queryParam("userId", invalidId).build())
                    .headers(TestUtils::authAdmin)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo(TestUtils.ID_NOT_POSITIVE);
        }

        @Test
        void returnBadRequest_whenUserIdIsMissing() {
            restClient.get()
                    .headers(TestUtils::authUser)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo("Request parameter 'userId' is missing.");
        }

        @Test
        void returnUnauthorized_whenNotAuthenticated() {
            restClient.get()
                    .uri(uri -> uri.queryParam("userId", 1L).build())
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

    }

}
