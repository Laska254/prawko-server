package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.model.Language;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.implementation.CategoryService;
import pl.prawko.prawko_server.service.implementation.LanguageService;
import pl.prawko.prawko_server.test_data.CategoryTestData;
import pl.prawko.prawko_server.test_data.LanguageTestData;
import pl.prawko.prawko_server.test_data.QuestionCSVTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionMapperTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private LanguageService languageService;

    @InjectMocks
    private QuestionMapper questionMapper;

    private final List<Language> languages = LanguageTestData.ALL;

    @BeforeEach
    void setUp() {
        final var answerMapper = new AnswerMapper(languageService);
        questionMapper = new QuestionMapper(categoryService, languageService, answerMapper);
        when(languageService.findAll()).thenReturn(languages);
    }

    @Test
    void mapQuestionCSVToQuestion_returnBasicQuestion() {
        final var categories = List.of(CategoryTestData.CATEGORY_A, CategoryTestData.CATEGORY_B);
        when(categoryService.findAllFromString("A,B")).thenReturn(categories);

        final var given = QuestionCSVTestData.createBasicQuestionCSV();
        final var expected = QuestionTestData.createQuestion(QuestionType.BASIC);

        final var result = questionMapper.mapQuestionCSVToQuestion(given);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void mapQuestionCSVToQuestion_returnSpecialQuestion() {
        final var category = CategoryTestData.CATEGORY_PT;
        when(categoryService.findAllFromString("PT")).thenReturn(List.of(category));
        final var given = QuestionCSVTestData.createSpecialQuestionCSV();
        final var expected = QuestionTestData.createQuestion(QuestionType.SPECIAL);

        final var result = questionMapper.mapQuestionCSVToQuestion(given);

        assertThat(result).isEqualTo(expected);
    }

}
