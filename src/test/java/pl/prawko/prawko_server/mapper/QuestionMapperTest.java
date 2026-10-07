package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.model.Language;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.test_data.QuestionCSVTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;
import pl.prawko.prawko_server.test_data.QuestionTranslationsTestData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuestionMapperTest {

    @Mock
    private AnswerMapper answerMapper;

    @InjectMocks
    private QuestionMapperImpl questionMapper;

    @ParameterizedTest
    @EnumSource(value = QuestionType.class)
    void toDto_mapAllFields_whenQuestionIsOfAnyType(QuestionType type) {
        final var given = QuestionTestData.createQuestion(type);
        final var expected = QuestionTestData.createQuestionDto(given);

        final var result = questionMapper.toDto(given, true);

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("answers")
                .isEqualTo(expected);
        verify(answerMapper, times(given.getAnswers().size())).toDto(any(), eq(true));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void toDto_passRevealCorrectToAnswerMapper_whenMappingAnswers(boolean revealCorrect) {
        final var given = QuestionTestData.createQuestion(QuestionType.SPECIAL);

        questionMapper.toDto(given, revealCorrect);

        given.getAnswers().forEach(answer -> verify(answerMapper).toDto(answer, revealCorrect));
    }

    @ParameterizedTest
    @MethodSource("pl.prawko.prawko_server.test_data.QuestionTranslationsTestData#translations")
    void toTranslationDto_mapContentAndLanguageCode_whenTranslationIsInAnyLanguage(Language language, String content) {
        final var given = QuestionTranslationsTestData.createTranslation(language, content);

        final var result = questionMapper.toTranslationDto(given);

        assertThat(result.languageCode()).isEqualTo(language.getCode());
        assertThat(result.content()).isEqualTo(content);
    }

    @ParameterizedTest
    @EnumSource(value = QuestionType.class)
    void toEntity_mapFieldsWithoutAssociations_whenCsvIsOfAnyType(QuestionType type) {
        final var given = QuestionCSVTestData.createQuestionCSV(type);
        final var expected = QuestionTestData.createQuestion(type);

        final var result = questionMapper.toEntity(given);

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("translations", "answers", "categories", "exams")
                .isEqualTo(expected);
        assertThat(result.getTranslations()).isNull();
        assertThat(result.getAnswers()).isNull();
        assertThat(result.getCategories()).isNull();
    }

}
