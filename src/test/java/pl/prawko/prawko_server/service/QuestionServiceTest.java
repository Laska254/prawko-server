package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.repository.QuestionRepository;
import pl.prawko.prawko_server.service.implementation.QuestionService;
import pl.prawko.prawko_server.test_data.QuestionTestData;
import pl.prawko.prawko_server.util.CSVParser;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QuestionServiceTest {

    @Mock
    private CSVParser parser;

    @Mock
    private QuestionRepository repository;

    @InjectMocks
    private QuestionService questionService;

    @Nested
    class ParseFileToQuestions {

        @Test
        void delegateParsingAndSaving() {
            final var file = mock(MultipartFile.class);
            final var parsedQuestions = List.of(new Question(), new Question());

            when(file.getOriginalFilename()).thenReturn("test.csv");
            when(parser.parse(file)).thenReturn(parsedQuestions);

            final var result = questionService.parseFileToQuestions(file);

            assertThat(result).isEqualTo(parsedQuestions);
            verify(parser).parse(file);
            verify(repository).saveAll(parsedQuestions);
        }

    }

    @Nested
    class GetAllByTypeAndCategory {

        @Test
        void returnListOfQuestions_whenFound() {
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
        void returnEmptyList_whenCategoryNotExists() {
            final var category = "Z";
            final var type = QuestionType.BASIC;
            when(repository.findByTypeAndCategories_NameContains(type, category)).thenReturn(Collections.emptyList());

            final var result = questionService.getAllByTypeAndCategory(type, category);

            assertThat(result).isEmpty();
            verify(repository).findByTypeAndCategories_NameContains(type, category);
            verifyNoMoreInteractions(repository);
        }

        @Test
        void returnEmptyList_whenTypeIsWrong() {
            final var category = "B";
            when(repository.findByTypeAndCategories_NameContains(null, category)).thenReturn(Collections.emptyList());

            final var result = questionService.getAllByTypeAndCategory(null, category);

            assertThat(result).isEmpty();
            verify(repository).findByTypeAndCategories_NameContains(null, category);
            verifyNoMoreInteractions(repository);
        }

    }

}
