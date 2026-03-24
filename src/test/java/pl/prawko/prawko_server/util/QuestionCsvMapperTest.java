package pl.prawko.prawko_server.util;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
public class QuestionCsvMapperTest {

    @Mock
    private LanguageService languageService;

    @InjectMocks
    private QuestionCsvMapper questionCsvMapper;

    @Test
    void fromQuestionCSVToAnswers_correctlyMapBasicAnswers() {
        final var question = QuestionTestData.createQuestion(QuestionType.BASIC);
        final var given = QuestionCSVTestData.createBasicQuestionCSV();
        final var expected = List.of(
                AnswerTestData.noAnswer(),
                AnswerTestData.yesAnswer()
        );

        final var result = questionCsvMapper.fromQuestionCSVToAnswers(given, question);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void fromQuestionsCSVToAnswers_correctlyMapSpecialAnswers() {
        final var given = QuestionCSVTestData.createSpecialQuestionCSV();
        final var question = QuestionTestData.createQuestion(QuestionType.SPECIAL);
        final var languages = LanguageTestData.ALL;
        final var expected = List.of(
                AnswerTestData.answerA(),
                AnswerTestData.answerB(),
                AnswerTestData.answerC()
        );
        when(languageService.findAll()).thenReturn(languages);

        final var result = questionCsvMapper.fromQuestionCSVToAnswers(given, question);

        assertThat(result).isEqualTo(expected);
    }

}

