package pl.prawko.prawko_server.model;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import pl.prawko.prawko_server.test_data.ExamTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EntityCascadeTest {

    @Autowired
    private EntityManager entityManager;

    private Question question;
    private User user;
    private Exam exam;

    @BeforeEach
    void setUp() {
        question = QuestionTestData.createQuestion(QuestionType.SPECIAL);
        entityManager.persist(question);
        user = UserTestData.createTestUserPippin();
        entityManager.persist(user);
        exam = ExamTestData.createExamWithoutQuestions(user)
                .setQuestions(List.of(question))
                .setUserAnswers(List.of(question.getAnswers().getFirst()));
        entityManager.persist(exam);
        flushAndClear();
    }

    @Test
    void persistingQuestion_cascadesToTranslationsAndAnswers() {
        assertThat(count(QuestionTranslation.class)).isEqualTo(question.getTranslations().size());
        assertThat(count(Answer.class)).isEqualTo(3);
        assertThat(count(AnswerTranslation.class)).isEqualTo(9);
    }

    @Test
    void removingUser_removesExams_butKeepsQuestionsAndAnswers() {
        entityManager.remove(entityManager.find(User.class, user.getId()));
        flushAndClear();

        assertThat(entityManager.find(Exam.class, exam.getId())).isNull();
        assertQuestionIsIntact();
    }

    @Test
    void removingExam_keepsQuestionsAndAnswers() {
        final var managedUser = entityManager.find(User.class, user.getId());
        managedUser.getExams().clear();
        flushAndClear();

        assertThat(entityManager.find(Exam.class, exam.getId())).isNull();
        assertThat(entityManager.find(User.class, user.getId())).isNotNull();
        assertQuestionIsIntact();
    }

    @Test
    void removingQuestion_removesTranslationsAndAnswers() {
        entityManager.remove(entityManager.find(Exam.class, exam.getId()));
        entityManager.remove(entityManager.find(Question.class, question.getId()));
        flushAndClear();

        assertThat(count(Question.class)).isZero();
        assertThat(count(QuestionTranslation.class)).isZero();
        assertThat(count(Answer.class)).isZero();
        assertThat(count(AnswerTranslation.class)).isZero();
    }

    @Test
    void removingAnswerFromQuestion_deletesOrphanWithTranslations() {
        final var managedQuestion = entityManager.find(Question.class, question.getId());
        final var unusedAnswer = managedQuestion.getAnswers().getLast();
        managedQuestion.getAnswers().remove(unusedAnswer);
        flushAndClear();

        assertThat(entityManager.find(Answer.class, unusedAnswer.getId())).isNull();
        assertThat(count(Answer.class)).isEqualTo(2);
        assertThat(count(AnswerTranslation.class)).isEqualTo(6);
    }

    @Test
    void removingTranslationFromQuestion_deletesOrphan() {
        final var managedQuestion = entityManager.find(Question.class, question.getId());
        final var translation = managedQuestion.getTranslations().getFirst();
        managedQuestion.getTranslations().remove(translation);
        flushAndClear();

        assertThat(entityManager.find(QuestionTranslation.class, translation.getId())).isNull();
        assertThat(count(QuestionTranslation.class)).isEqualTo(question.getTranslations().size() - 1);
    }

    @Test
    void removingTranslationFromAnswer_deletesOrphan() {
        final var managedAnswer = entityManager.find(Question.class, question.getId()).getAnswers().getFirst();
        final var translation = managedAnswer.getTranslations().getFirst();
        managedAnswer.getTranslations().remove(translation);
        flushAndClear();

        assertThat(entityManager.find(AnswerTranslation.class, translation.getId())).isNull();
        assertThat(count(AnswerTranslation.class)).isEqualTo(8);
    }

    private void assertQuestionIsIntact() {
        final var reloaded = entityManager.find(Question.class, question.getId());
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getAnswers()).hasSize(3);
        assertThat(reloaded.getTranslations()).hasSize(question.getTranslations().size());
        assertThat(count(AnswerTranslation.class)).isEqualTo(9);
    }

    private long count(final Class<?> entity) {
        return entityManager.createQuery("select count(e) from " + entity.getSimpleName() + " e", Long.class)
                .getSingleResult();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

}
