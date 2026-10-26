package pl.prawko.prawko_server.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.CategoryVariant;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.repository.QuestionRepository;
import pl.prawko.prawko_server.service.implementation.QuestionService;
import pl.prawko.prawko_server.test_data.QuestionTestData;
import pl.prawko.prawko_server.util.CSVParser;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QuestionServiceTest {

    @Mock
    private CSVParser parser;

    @Mock
    private QuestionRepository repository;

    @Mock
    private QuestionMapper mapper;

    @InjectMocks
    private QuestionService questionService;

    @Nested
    class ParseFileToQuestions {

        @Test
        void parseAndSaveQuestions_whenFileIsCsv() {
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
            final var category = CategoryVariant.B;
            final var expected = List.of(question);
            when(repository.findByTypeAndCategories_Name(question.getType(), category)).thenReturn(expected);

            final var result = questionService.getAllByTypeAndCategory(question.getType(), category);

            assertThat(result).isEqualTo(expected);
            verify(repository).findByTypeAndCategories_Name(question.getType(), category);
            verifyNoMoreInteractions(repository);
        }

        @Test
        void returnEmptyList_whenCategoryHasNoQuestions() {
            final var category = CategoryVariant.T;
            final var type = QuestionType.BASIC;
            when(repository.findByTypeAndCategories_Name(type, category)).thenReturn(Collections.emptyList());

            final var result = questionService.getAllByTypeAndCategory(type, category);

            assertThat(result).isEmpty();
            verify(repository).findByTypeAndCategories_Name(type, category);
            verifyNoMoreInteractions(repository);
        }

        @Test
        void returnEmptyList_whenTypeIsWrong() {
            final var category = CategoryVariant.B;
            when(repository.findByTypeAndCategories_Name(null, category)).thenReturn(Collections.emptyList());

            final var result = questionService.getAllByTypeAndCategory(null, category);

            assertThat(result).isEmpty();
            verify(repository).findByTypeAndCategories_Name(null, category);
            verifyNoMoreInteractions(repository);
        }

    }

    @Nested
    class GetById {

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void returnDtoMappedWithRevealCorrect_whenQuestionExists(final boolean revealCorrect) {
            final var question = QuestionTestData.createQuestion(QuestionType.BASIC);
            final var dto = QuestionTestData.createQuestionDto(question);
            when(repository.findById(question.getId())).thenReturn(Optional.of(question));
            when(mapper.toDto(question, revealCorrect)).thenReturn(dto);

            final var result = questionService.getById(question.getId(), revealCorrect);

            assertThat(result).isEqualTo(dto);
        }

        @Test
        void throwEntityNotFound_whenQuestionDoesNotExist() {
            final var nonExistentId = 666L;
            when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> questionService.getById(nonExistentId, false))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(QuestionTestData.questionNotFoundMessage(nonExistentId));
            verifyNoInteractions(mapper);
        }

    }

    @Nested
    class GetRandom {

        @Test
        void returnDtoWithCorrectAnswersAtRandomOffset_whenQuestionsMatch() {
            final var question = QuestionTestData.createQuestion(QuestionType.BASIC);
            final var dto = QuestionTestData.createQuestionDto(question);
            final var category = CategoryVariant.B;
            final var count = 3L;
            final var pageCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(repository.countMatching(question.getType(), category.name())).thenReturn(count);
            when(repository.findMatching(eq(question.getType()), eq(category.name()), pageCaptor.capture()))
                    .thenReturn(List.of(question));
            when(mapper.toDto(question, true)).thenReturn(dto);

            final var result = questionService.getRandom(category, question.getType());

            assertThat(result).isEqualTo(dto);
            final var page = pageCaptor.getValue();
            assertThat(page.getPageNumber()).isBetween(0, (int) count - 1);
            assertThat(page.getPageSize()).isOne();
        }

        @Test
        void matchAnyQuestion_whenCategoryAndTypeAreNull() {
            final var question = QuestionTestData.createQuestion(QuestionType.BASIC);
            final var dto = QuestionTestData.createQuestionDto(question);
            when(repository.countMatching(null, null)).thenReturn(1L);
            when(repository.findMatching(isNull(), isNull(), any(Pageable.class))).thenReturn(List.of(question));
            when(mapper.toDto(question, true)).thenReturn(dto);

            final var result = questionService.getRandom(null, null);

            assertThat(result).isEqualTo(dto);
        }

        @ParameterizedTest
        @CsvSource({"B, SPECIAL", ","})
        void throwEntityNotFound_whenNoQuestionMatches(final CategoryVariant category, final QuestionType type) {
            final var categoryName = Objects.toString(category, null);
            when(repository.countMatching(type, categoryName)).thenReturn(0L);

            assertThatThrownBy(() -> questionService.getRandom(category, type))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(QuestionTestData.noQuestionFoundMessage(category, type));
            verify(repository, never()).findMatching(any(), any(), any());
            verifyNoInteractions(mapper);
        }

        @Test
        void throwEntityNotFound_whenQuestionIsDeletedAfterCount() {
            when(repository.countMatching(null, null)).thenReturn(1L);
            when(repository.findMatching(isNull(), isNull(), any(Pageable.class))).thenReturn(List.of());

            assertThatThrownBy(() -> questionService.getRandom(null, null))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(QuestionTestData.noQuestionFoundMessage(null, null));
            verifyNoInteractions(mapper);
        }

    }

    @Nested
    class GetAll {

        @Test
        void returnPageOfDtos_whenQuestionsExist() {
            final var question = QuestionTestData.createQuestion(QuestionType.SPECIAL);
            final var dto = QuestionTestData.createQuestionDto(question);
            final var pageable = PageRequest.of(2, 1);
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(question), pageable, 3));
            when(mapper.toDto(question, true)).thenReturn(dto);

            final var result = questionService.getAll(pageable);

            assertThat(result.getContent()).containsExactly(dto);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getPageable()).isEqualTo(pageable);
        }

        @Test
        void returnEmptyPage_whenNoQuestionsExist() {
            final var pageable = PageRequest.of(0, 20);
            when(repository.findAll(pageable)).thenReturn(Page.empty(pageable));

            final var result = questionService.getAll(pageable);

            assertThat(result).isEmpty();
            verifyNoMoreInteractions(mapper);
        }

    }

}
