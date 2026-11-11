package pl.prawko.prawko_server.util;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.ILanguageService;
import pl.prawko.prawko_server.testdata.AnswerTestData;
import pl.prawko.prawko_server.testdata.LanguageTestData;
import pl.prawko.prawko_server.testdata.QuestionCsvTestData;
import pl.prawko.prawko_server.testdata.QuestionTestData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnswerFactoryTest {

    @Mock
    private ILanguageService languageService;

    @InjectMocks
    private AnswerFactory answerFactory;

    @ParameterizedTest
    @EnumSource(QuestionType.class)
    void create_returnAnswers_whenCsvIsValid(final QuestionType type) {
        final var csv = QuestionCsvTestData.createQuestionCsv(type);
        final var question = QuestionTestData.createQuestion(type);
        final var expected = AnswerTestData.createAnswers(type);
        if (type == QuestionType.SPECIAL) {
            when(languageService.findAll()).thenReturn(LanguageTestData.ALL);
        }

        final var result = answerFactory.create(csv, question);

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("question", "translations.answer")
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"T, true, false", "N, false, true"})
    void create_returnYesThenNo_whenQuestionIsBasic(final char correctAnswer,
                                                    final boolean yesCorrect,
                                                    final boolean noCorrect) {
        final var csv = QuestionCsvTestData.createBasicQuestionCsv(correctAnswer);
        final var question = QuestionTestData.createQuestion(QuestionType.BASIC);

        final var result = answerFactory.create(csv, question);

        assertThat(result)
                .extracting(Answer::isCorrect)
                .containsExactly(yesCorrect, noCorrect);
    }

}
