package pl.prawko.prawko_server.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.test_data.CategoryTestData;
import pl.prawko.prawko_server.test_data.TestDataFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
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

    private final TestDataFactory testDataFactory = new TestDataFactory();

    @Test
    void saveAll_correctly() {
        final var question1 = testDataFactory.createQuestion(QuestionType.BASIC);
        final var question2 = testDataFactory.createQuestion(QuestionType.SPECIAL);
        final var expected = List.of(question1, question2);
        repository.saveAll(expected);
        final var result = repository.findAll();

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(expected);
    }

    @Test
    void findByTypeAndCategoriesContaining() {
        final var question = testDataFactory.createQuestion(QuestionType.BASIC);
        final var category = CategoryTestData.CATEGORY_B;
        repository.save(question);
        final var expected = List.of(question);
        final var result = repository.findByTypeAndCategories_NameContains(question.getType(), category.getName());

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(expected);
    }

}
