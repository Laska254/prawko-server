package pl.prawko.prawko_server.util;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.implementation.LanguageService;
import pl.prawko.prawko_server.test_data.LanguageTestData;
import pl.prawko.prawko_server.test_data.QuestionCSVTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;
import pl.prawko.prawko_server.test_data.QuestionTranslationsTestData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionTranslationFactoryTest {

    @Mock
    private LanguageService languageService;

    @InjectMocks
    private QuestionTranslationFactory factory;

    @ParameterizedTest
    @EnumSource(QuestionType.class)
    void createTranslationTranslations(QuestionType type) {
        final var questionCSV = QuestionCSVTestData.createQuestionCSV(type);
        final var question = QuestionTestData.createQuestion(type);
        final var expected = QuestionTranslationsTestData.createTranslations(type);
        when(languageService.findAll()).thenReturn(LanguageTestData.ALL);

        final var result = factory.createTranslations(questionCSV, question);

        assertThat(result).isEqualTo(expected);
    }

}