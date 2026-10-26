package pl.prawko.prawko_server.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import pl.prawko.prawko_server.model.CategoryVariant;
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
    void findByTypeAndCategoriesName_returnQuestions_whenTypeAndCategoryMatch() {
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

    @Test
    void findByTypeAndCategoriesName_returnEmpty_whenOnlyCategoryContainingNameMatches() {
        final var question = repository.save(QuestionTestData.createQuestion(QuestionType.SPECIAL));

        final var result = repository.findByTypeAndCategories_Name(question.getType(), CategoryVariant.T);

        assertThat(result).isEmpty();
    }

    @Test
    void countByTypeAndCategoriesName_returnCount_whenTypeAndCategoryMatch() {
        final var question = repository.save(QuestionTestData.createQuestion(QuestionType.BASIC));
        final var category = question.getCategories().getFirst().getName();

        assertThat(repository.countByTypeAndCategories_Name(question.getType(), category)).isOne();
        assertThat(repository.countByTypeAndCategories_Name(QuestionType.SPECIAL, category)).isZero();
    }

    @Test
    void findByTypeAndCategoriesName_returnQuestionAtOffset_whenPaged() {
        final var questions = Stream.concat(
                        QuestionTestData.createQuestions(QuestionType.SPECIAL).stream(),
                        QuestionTestData.createQuestions(QuestionType.BASIC).stream())
                .toList();
        repository.saveAll(questions);
        final var page = PageRequest.of(1, 1, Sort.by("id"));

        final var result = repository.findByTypeAndCategories_Name(
                QuestionType.SPECIAL, CategoryTestData.CATEGORY_B.getName(), page);

        assertThat(result).extracting(Question::getId).containsExactly(5L);
    }

}
