package pl.prawko.prawko_server.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.Exam;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.test_data.ExamTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;
import pl.prawko.prawko_server.test_data.UserTestData;

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

    private Exam loadDetached(final long id) {
        final var exam = repository.findById(id).orElseThrow();
        exam.getUserAnswers().size();
        entityManager.detach(exam);
        return exam;
    }

}
