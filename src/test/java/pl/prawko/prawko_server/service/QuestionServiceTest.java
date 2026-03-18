package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.repository.QuestionRepository;
import pl.prawko.prawko_server.service.implementation.QuestionService;
import pl.prawko.prawko_server.test_data.QuestionTestData;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QuestionServiceTest {

    @Mock
    private QuestionRepository repository;

    @InjectMocks
    private QuestionService questionService;

    @Test
    void saveAll_callsRepositorySaveAll() {
        final var questions = List.of(QuestionTestData.createQuestion(QuestionType.BASIC), QuestionTestData.createQuestion(QuestionType.SPECIAL));

        questionService.saveAll(questions);

        verify(repository).saveAll(questions);
    }

    @Test
    void getAllByTypeAndCategory_returnListOfQuestions_whenFound() {
        final var question = QuestionTestData.createQuestion(QuestionType.BASIC);
        final var category = "B";
        final var expected = List.of(question);
        when(repository.findByTypeAndCategories_NameContains(question.getType(), category)).thenReturn(expected);

        final var result = questionService.getAllByTypeAndCategory(question.getType(), category);

        assertThat(result).isEqualTo(expected);
        verify(repository).findByTypeAndCategories_NameContains(question.getType(), category);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getAllByTypeAndCategory_returnEmptyList_whenCategoryNotExists() {
        final var category = "Z";
        final var type = QuestionType.BASIC;
        when(repository.findByTypeAndCategories_NameContains(type, category)).thenReturn(Collections.emptyList());

        final var result = questionService.getAllByTypeAndCategory(type, category);

        assertThat(result).isEmpty();
        verify(repository).findByTypeAndCategories_NameContains(type, category);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getAllByTypeAndCategory_returnEmptyList_whenTypeIsWrong() {
        final var category = "B";
        when(repository.findByTypeAndCategories_NameContains(null, category)).thenReturn(Collections.emptyList());

        final var result = questionService.getAllByTypeAndCategory(null, category);

        assertThat(result).isEmpty();
        verify(repository).findByTypeAndCategories_NameContains(null, category);
        verifyNoMoreInteractions(repository);
    }

}
