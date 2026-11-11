package pl.prawko.prawko_server.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import pl.prawko.prawko_server.exception.UnknownCorrectAnswerException;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCsv;
import pl.prawko.prawko_server.model.QuestionTranslation;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.ICategoryService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CsvFacadeTest {

    private static final String TYPE_WARNING = "'Zakres struktury' doesn't name the type";

    @Mock
    private ICategoryService categoryService;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private QuestionTranslationFactory translationFactory;

    @Mock
    private AnswerFactory answerFactory;

    @InjectMocks
    private CsvFacade facade;

    @ParameterizedTest
    @EnumSource(QuestionType.class)
    void mapSingleRow_returnQuestionWithAssociations_whenCsvIsValid(final QuestionType type) {
        final var questionCsv = mock(QuestionCsv.class);
        final var question = new Question();
        final var translations = List.of(new QuestionTranslation());
        final var answers = List.of(new Answer());
        final var categories = List.of(new Category());

        switch (type) {
            case BASIC -> {
                when(questionCsv.correctAnswer()).thenReturn('N');
                when(questionCsv.categories()).thenReturn("A,B");
            }
            case SPECIAL -> {
                when(questionCsv.correctAnswer()).thenReturn('B');
                when(questionCsv.categories()).thenReturn("PT");
            }
        }
        when(questionCsv.type()).thenReturn(type.getName());
        when(questionMapper.toEntity(questionCsv)).thenReturn(question);
        when(translationFactory.createTranslations(questionCsv, question)).thenReturn(translations);
        when(answerFactory.create(questionCsv, question)).thenReturn(answers);
        when(categoryService.findAllFromString(questionCsv.categories())).thenReturn(categories);

        final var result = facade.mapSingleRow(questionCsv);

        assertThat(result).isSameAs(question);
        assertThat(result.getType()).isEqualTo(type);
        assertThat(result.getTranslations()).isSameAs(translations);
        assertThat(result.getAnswers()).isSameAs(answers);
        assertThat(result.getCategories()).isSameAs(categories);

        verify(questionMapper).toEntity(questionCsv);
        verify(translationFactory).createTranslations(questionCsv, question);
        verify(answerFactory).create(questionCsv, question);
        switch (type) {
            case BASIC -> verify(categoryService).findAllFromString("A,B");
            case SPECIAL -> verify(categoryService).findAllFromString("PT");
        }
        verifyNoMoreInteractions(questionMapper, translationFactory, answerFactory, categoryService);
    }

    @ParameterizedTest
    @CsvSource({
            "T, SPECJALISTYCZNY, BASIC",
            "A, PODSTAWOWY, SPECIAL",
            "C, Specajlistyczny, SPECIAL",
            "N, '', BASIC"
    })
    void mapSingleRow_takeTypeFromCorrectAnswerAndWarn_whenTypeDoesNotMatch(final char correctAnswer,
                                                                           final String typeName,
                                                                           final QuestionType expected,
                                                                           final CapturedOutput output) {
        final var questionCsv = mockQuestionCsv(correctAnswer, typeName);
        when(questionCsv.id()).thenReturn(7);

        final var result = facade.mapSingleRow(questionCsv);

        assertThat(result.getType()).isEqualTo(expected);
        assertThat(output).contains("Question 7: " + TYPE_WARNING + " of correct answer '" + correctAnswer
                + "', importing it as " + expected);
    }

    @Test
    void mapSingleRow_notWarn_whenTypeDiffersOnlyInCase(final CapturedOutput output) {
        final var questionCsv = mockQuestionCsv('C', "Specjalistyczny");

        final var result = facade.mapSingleRow(questionCsv);

        assertThat(result.getType()).isEqualTo(QuestionType.SPECIAL);
        assertThat(output).doesNotContain(TYPE_WARNING);
    }

    @Test
    void mapSingleRow_throwUnknownCorrectAnswerException_whenCorrectAnswerIsUnknown() {
        final var questionCsv = mock(QuestionCsv.class);
        when(questionCsv.correctAnswer()).thenReturn('X');

        assertThatThrownBy(() -> facade.mapSingleRow(questionCsv))
                .isInstanceOf(UnknownCorrectAnswerException.class)
                .hasMessage("Unknown correct answer: X");
        verifyNoInteractions(questionMapper, translationFactory, answerFactory, categoryService);
    }

    private QuestionCsv mockQuestionCsv(final char correctAnswer, final String typeName) {
        final var questionCsv = mock(QuestionCsv.class);
        when(questionCsv.correctAnswer()).thenReturn(correctAnswer);
        when(questionCsv.type()).thenReturn(typeName);
        when(questionMapper.toEntity(questionCsv)).thenReturn(new Question());
        return questionCsv;
    }

}
