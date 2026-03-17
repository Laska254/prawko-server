package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.model.Language;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.implementation.LanguageService;
import pl.prawko.prawko_server.test_data.AnswerTestData;
import pl.prawko.prawko_server.test_data.LanguageTestData;
import pl.prawko.prawko_server.test_data.QuestionCSVTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnswerMapperTest {

    @Mock
    private LanguageService languageService;

    @InjectMocks
    private AnswerMapper mapper;

    private final List<Language> languages = LanguageTestData.ALL;

    @BeforeEach
    void setUp() {
        mapper = new AnswerMapper(languageService);
    }

    @Test
    void fromQuestionCSVToAnswers_correctlyMapBasicAnswers() {
        final var question = QuestionTestData.createQuestion(QuestionType.BASIC);
        final var given = QuestionCSVTestData.createBasicQuestionCSV();
        final var expected = List.of(
                AnswerTestData.noAnswer(),
                AnswerTestData.yesAnswer()
        );

        final var result = mapper.fromQuestionCSVToAnswers(given, question);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void fromQuestionsCSVToAnswers_correctlyMapSpecialAnswers() {
        final var given = QuestionCSVTestData.createSpecialQuestionCSV();
        final var question = QuestionTestData.createQuestion(QuestionType.SPECIAL);
        final var expected = List.of(
                AnswerTestData.answerA(),
                AnswerTestData.answerB(),
                AnswerTestData.answerC()
        );
        when(languageService.findAll()).thenReturn(languages);

        final var result = mapper.fromQuestionCSVToAnswers(given, question);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toDto_correctlyMapAnswer() {
        final var given = QuestionTestData.createQuestion(QuestionType.SPECIAL)
                .getAnswers()
                .getFirst();
        final var expected = AnswerTestData.createAnswerDtoA();

        final var result = mapper.toDto(given);

        assertThat(result).isEqualTo(expected);
    }

}
