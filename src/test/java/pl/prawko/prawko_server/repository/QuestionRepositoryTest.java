package pl.prawko.prawko_server.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.test_data.CategoryTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class QuestionRepositoryTest {

    private static final String[] IGNORED_FIELDS = {
            "answers.id",
            "answers.translations.id",
            "answers.translations.language.answerTranslations",
            "answers.translations.language.questionTranslations",
            "answers.translations.language.exams",
            "translations.id",
            "translations.language",
            "categories.questions",
            "categories.exams"};

    @Autowired
    private QuestionRepository repository;

    @Test
    void saveAll_persistQuestions_whenQuestionsAreValid() {
        final var question1 = QuestionTestData.createQuestion(QuestionType.BASIC);
        final var question2 = QuestionTestData.createQuestion(QuestionType.SPECIAL);
        final var expected = List.of(question1, question2);
        repository.saveAll(expected);
        final var result = repository.findAll();

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(expected);
    }

    @Test
    void findByTypeAndCategoriesNameContains_returnQuestions_whenTypeAndCategoryMatch() {
        final var question = QuestionTestData.createQuestion(QuestionType.BASIC);
        final var category = CategoryTestData.CATEGORY_B;
        repository.save(question);
        final var expected = List.of(question);
        final var result = repository.findByTypeAndCategories_NameContains(question.getType(), category.getName());

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(expected);
    }

    @Test
    void countMatching_returnCount_whenTypeAndCategoryMatch() {
        final var question = repository.save(QuestionTestData.createQuestion(QuestionType.BASIC));
        final var category = question.getCategories().getFirst().getName();

        assertThat(repository.countMatching(question.getType(), category)).isOne();
        assertThat(repository.countMatching(QuestionType.SPECIAL, category)).isZero();
    }

    @Test
    void countMatching_returnZero_whenCategoryOnlyContainsName() {
        final var question = repository.save(QuestionTestData.createQuestion(QuestionType.SPECIAL));
        final var partialName = CategoryTestData.CATEGORY_PT.getName().substring(1);

        assertThat(repository.countMatching(question.getType(), partialName)).isZero();
    }

    @Test
    void countMatching_matchAnyValueAndCountQuestionOnce_whenParameterIsNull() {
        final var multiCategory = QuestionTestData.createQuestion(QuestionType.BASIC);
        final var special = QuestionTestData.createQuestion(QuestionType.SPECIAL);
        repository.saveAll(List.of(multiCategory, special));

        assertThat(repository.countMatching(null, null)).isEqualTo(2);
        assertThat(repository.countMatching(multiCategory.getType(), null)).isOne();
        assertThat(repository.countMatching(null, CategoryTestData.CATEGORY_PT.getName())).isOne();
    }

    @Test
    void findMatching_returnQuestionAtOffsetOrderedById_whenTypeAndCategoryMatch() {
        final var questions = Stream.concat(
                        QuestionTestData.createQuestions(QuestionType.SPECIAL).stream(),
                        QuestionTestData.createQuestions(QuestionType.BASIC).stream())
                .toList();
        repository.saveAll(questions);
        final var page = PageRequest.of(1, 1);

        final var any = repository.findMatching(null, null, page);
        final var specialInB = repository.findMatching(QuestionType.SPECIAL, CategoryTestData.CATEGORY_B.getName(), page);

        assertThat(any).extracting(Question::getId).containsExactly(2L);
        assertThat(specialInB).extracting(Question::getId).containsExactly(5L);
    }

}
