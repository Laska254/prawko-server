package pl.prawko.prawko_server.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.test_data.CategoryTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;

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

    @Test
    void saveAll_correctly() {
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
    void findByTypeAndCategories_Name_matchesOnlyExactCategoryName() {
        final var questionB1 = new Question()
                .setId(501L)
                .setType(QuestionType.BASIC)
                .setCategories(List.of(CategoryTestData.CATEGORY_B1));
        final var questionPT = new Question()
                .setId(502L)
                .setType(QuestionType.BASIC)
                .setCategories(List.of(CategoryTestData.CATEGORY_PT));
        repository.saveAll(List.of(questionB1, questionPT));

        assertThat(repository.findByTypeAndCategories_Name(QuestionType.BASIC, "B")).isEmpty();
        assertThat(repository.findByTypeAndCategories_Name(QuestionType.BASIC, "T")).isEmpty();
        assertThat(repository.findByTypeAndCategories_Name(QuestionType.BASIC, "B1"))
                .extracting(Question::getId)
                .containsExactly(501L);
    }

    @Test
    void findByTypeAndCategoriesContaining() {
        final var question = QuestionTestData.createQuestion(QuestionType.BASIC);
        final var category = CategoryTestData.CATEGORY_B;
        repository.save(question);
        final var expected = List.of(question);
        final var result = repository.findByTypeAndCategories_Name(question.getType(), category.getName());

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(expected);
    }

}
