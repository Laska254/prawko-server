package pl.prawko.prawko_server.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.model.Language;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.ILanguageService;
import pl.prawko.prawko_server.testdata.LanguageTestData;
import pl.prawko.prawko_server.testdata.QuestionCsvTestData;
import pl.prawko.prawko_server.testdata.QuestionTestData;
import pl.prawko.prawko_server.testdata.QuestionTranslationsTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionTranslationFactoryTest {

    @Mock
    private ILanguageService languageService;

    @InjectMocks
    private QuestionTranslationFactory factory;

    @ParameterizedTest
    @EnumSource(QuestionType.class)
    void createTranslations_returnTranslations_whenCsvIsValid(final QuestionType type) {
        final var questionCsv = QuestionCsvTestData.createQuestionCsv(type);
        final var question = QuestionTestData.createQuestion(type);
        final var expected = QuestionTranslationsTestData.createTranslations(type);
        when(languageService.findAll()).thenReturn(LanguageTestData.ALL);

        final var result = factory.createTranslations(questionCsv, question);

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("question")
                .isEqualTo(expected);
    }

    @Test
    void createTranslations_throwIllegalState_whenLanguageIsNotSupported() {
        final var type = QuestionType.BASIC;
        final var unsupported = new Language().setId(4L).setName("french").setCode("fr");
        when(languageService.findAll()).thenReturn(List.of(unsupported));

        assertThatThrownBy(() -> factory.createTranslations(
                QuestionCsvTestData.createQuestionCsv(type), QuestionTestData.createQuestion(type)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unexpected language: fr");
    }

}
