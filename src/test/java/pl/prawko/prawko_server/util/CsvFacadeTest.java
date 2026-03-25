package pl.prawko.prawko_server.util;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCSV;
import pl.prawko.prawko_server.model.QuestionTranslation;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.implementation.CategoryService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvFacadeTest {

    @Mock
    private CategoryService categoryService;

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
    void shouldMapSingleRow(QuestionType type) {
        final var questionCsv = mock(QuestionCSV.class);
        final var question = new Question();
        final var translations = List.of(new QuestionTranslation());
        final var answers = List.of(new Answer());
        final var categories = List.of(new Category());

        switch (type) {
            case BASIC -> when(questionCsv.categories()).thenReturn("A,B");
            case SPECIAL -> when(questionCsv.categories()).thenReturn("PT");
        }
        when(questionMapper.toEntity(questionCsv)).thenReturn(question);
        when(translationFactory.createTranslations(questionCsv, question)).thenReturn(translations);
        when(answerFactory.create(questionCsv, question)).thenReturn(answers);
        when(categoryService.findAllFromString(questionCsv.categories())).thenReturn(categories);

        final var result = facade.mapSingleRow(questionCsv);

        assertThat(result).isSameAs(question);
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

}