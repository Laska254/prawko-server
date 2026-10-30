package pl.prawko.prawko_server.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.Exam;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.test_data.CategoryTestData;
import pl.prawko.prawko_server.test_data.ExamTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class ExamRepositoryTest {

    @Autowired
    private ExamRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveAndFlush_throwOptimisticLockingFailure_whenUserAnswersWereChangedConcurrently() {
        final var question = QuestionTestData.createQuestion(QuestionType.SPECIAL);
        entityManager.persist(question);
        final var user = UserTestData.createTestUserPippin();
        entityManager.persist(user);
        final var saved = repository.saveAndFlush(ExamTestData.createExamWithoutQuestions(user).setQuestions(List.of(question)));
        entityManager.clear();
        final var stale = loadDetached(saved.getId());

        final var current = repository.findById(saved.getId()).orElseThrow();
        current.getUserAnswers().add(question.getAnswers().getFirst());
        repository.flush();
        entityManager.clear();
        stale.getUserAnswers().add(question.getAnswers().getLast());

        assertThatThrownBy(() -> repository.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        entityManager.clear();
        assertThat(repository.findById(saved.getId()).orElseThrow().getUserAnswers())
                .extracting(Answer::getId)
                .containsExactly(question.getAnswers().getFirst().getId());
    }

    @Test
    void findAllByActiveTrueAndExpiresBefore_returnOnlyActiveExpiredExams() {
        final var user = UserTestData.createTestUserPippin();
        entityManager.persist(user);
        final var expired = repository.save(ExamTestData.expire(ExamTestData.createExamWithoutQuestions(user)));
        repository.save(ExamTestData.expire(ExamTestData.createExamWithoutQuestions(user)).setActive(false));
        repository.save(ExamTestData.createExamWithoutQuestions(user));
        entityManager.clear();

        final var result = repository.findAllByActiveTrueAndExpiresBefore(Instant.now());

        assertThat(result)
                .extracting(Exam::getId)
                .containsExactly(expired.getId());
    }

    @Test
    void findAllByUser_IdAndCategory_Name_returnOnlyUserExamsOfCategory() {
        final var user = UserTestData.createTestUserPippin();
        entityManager.persist(user);
        final var other = UserTestData.createMerry();
        entityManager.persist(other);
        final var matching = repository.save(ExamTestData.createExamWithoutQuestions(user));
        repository.save(ExamTestData.createExamWithoutQuestions(user).setCategory(CategoryTestData.CATEGORY_A));
        repository.save(ExamTestData.createExamWithoutQuestions(other));
        entityManager.clear();

        final var result = repository.findAllByUser_IdAndCategory_Name(user.getId(), CategoryTestData.CATEGORY_B.getName(),
                PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(Exam::getId)
                .containsExactly(matching.getId());
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findTotalsByUserId_aggregateOnlyFinishedExamsOfUser() {
        final var user = UserTestData.createTestUserPippin();
        entityManager.persist(user);
        final var other = UserTestData.createMerry();
        entityManager.persist(other);
        repository.saveAll(ExamTestData.createExamsForStats(user));
        repository.save(ExamTestData.finish(ExamTestData.createExamWithoutQuestions(other), 74));
        entityManager.clear();

        final var result = repository.findTotalsByUserId(user.getId(), Exam.PASSING_SCORE);

        final var expected = ExamTestData.STATS;
        assertThat(result.getExamsTaken()).isEqualTo(expected.examsTaken());
        assertThat(result.getExamsPassed()).isEqualTo(expected.examsPassed());
        assertThat(result.getAverageScore()).isEqualTo(expected.averageScore());
        assertThat(result.getBestScore()).isEqualTo(expected.bestScore());
    }

    @Test
    void findTotalsByUserId_returnZeroCountsAndNullScores_whenUserHasNoFinishedExams() {
        final var user = UserTestData.createTestUserPippin();
        entityManager.persist(user);
        repository.save(ExamTestData.createExamWithoutQuestions(user));
        entityManager.clear();

        final var result = repository.findTotalsByUserId(user.getId(), Exam.PASSING_SCORE);

        assertThat(result.getExamsTaken()).isZero();
        assertThat(result.getExamsPassed()).isZero();
        assertThat(result.getAverageScore()).isNull();
        assertThat(result.getBestScore()).isNull();
    }

    @Test
    void findCategoryStatsByUserId_aggregateOnlyFinishedExamsOfUserPerCategory() {
        final var user = UserTestData.createTestUserPippin();
        entityManager.persist(user);
        final var other = UserTestData.createMerry();
        entityManager.persist(other);
        repository.saveAll(ExamTestData.createExamsForStats(user));
        repository.save(ExamTestData.finish(ExamTestData.createExamWithoutQuestions(other), 74));
        entityManager.clear();

        final var result = repository.findCategoryStatsByUserId(user.getId(), Exam.PASSING_SCORE);

        assertThat(result).isEqualTo(ExamTestData.STATS.categories());
    }

    @Test
    void findCategoryStatsByUserId_returnEmptyList_whenUserHasNoFinishedExams() {
        final var user = UserTestData.createTestUserPippin();
        entityManager.persist(user);
        repository.save(ExamTestData.createExamWithoutQuestions(user));
        entityManager.clear();

        final var result = repository.findCategoryStatsByUserId(user.getId(), Exam.PASSING_SCORE);

        assertThat(result).isEmpty();
    }

    private Exam loadDetached(final long id) {
        final var exam = repository.findById(id).orElseThrow();
        exam.getUserAnswers().size();
        entityManager.detach(exam);
        return exam;
    }

}
