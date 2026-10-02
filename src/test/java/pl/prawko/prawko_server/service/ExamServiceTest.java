package pl.prawko.prawko_server.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import pl.prawko.prawko_server.mapper.ExamMapper;
import pl.prawko.prawko_server.model.Exam;
import pl.prawko.prawko_server.repository.ExamRepository;
import pl.prawko.prawko_server.service.implementation.CategoryService;
import pl.prawko.prawko_server.service.implementation.ExamService;
import pl.prawko.prawko_server.service.implementation.QuestionService;
import pl.prawko.prawko_server.service.implementation.UserService;
import pl.prawko.prawko_server.test_data.CategoryTestData;
import pl.prawko.prawko_server.test_data.ExamTestData;
import pl.prawko.prawko_server.test_data.UserTestData;
import pl.prawko.prawko_server.util.ExamGenerator;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExamServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private QuestionService questionService;

    @Mock
    private ExamRepository repository;

    @Mock
    private ExamMapper examMapper;

    @Mock
    private ExamGenerator examGenerator;

    @InjectMocks
    private ExamService service;

    @Nested
    class CreateExam {

        @Test
        void returnExamId_whenExamIsCreated() {
            final var user = UserTestData.createTestUserPippin();
            final var category = CategoryTestData.CATEGORY_PT;
            final var exam = ExamTestData.createExam(user);

            when(userService.getById(user.getId())).thenReturn(user);
            when(categoryService.findByName(category.getName())).thenReturn(category);
            when(repository.save(any(Exam.class))).thenAnswer(inv -> {
                inv.getArgument(0, Exam.class).setId(1L);
                return null;
            });

            final var examId = service.createExam(user.getId(), category.getName());

            assertThat(examId).isEqualTo(1L);
            repository.delete(exam);
        }

        @Test
        void addExamToUserExamsList_whenSuccess() {
            final var user = UserTestData.createTestUserPippin();
            final var category = CategoryTestData.CATEGORY_PT;

            when(userService.getById(user.getId())).thenReturn(user);
            when(categoryService.findByName(category.getName())).thenReturn(category);
            when(examGenerator.generate(category)).thenReturn(Collections.emptyList());

            service.createExam(user.getId(), category.getName());

            assertThat(user.getExams()).hasSize(1);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            final long nonExistentUserId = 666L;
            final var categoryName = CategoryTestData.CATEGORY_B.getName();
            final var expectedMessage = UserTestData.userNotFoundMessage(nonExistentUserId);
            when(userService.getById(nonExistentUserId)).thenThrow(new EntityNotFoundException(expectedMessage));

            assertThatThrownBy(() -> service.createExam(nonExistentUserId, categoryName))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(expectedMessage);

            verifyNoInteractions(repository, categoryService, questionService, examMapper, examGenerator);
        }

        @Test
        void shouldThrowWhenCategoryNotFound() {
            final var user = UserTestData.createTestUserPippin();
            final var nonExistentCategory = "NON_EXISTENT";

            when(userService.getById(user.getId())).thenReturn(user);
            when(categoryService.findByName(nonExistentCategory)).thenThrow(new EntityNotFoundException("Category not found"));

            assertThatThrownBy(() -> service.createExam(user.getId(), nonExistentCategory))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Category not found");

            verifyNoInteractions(repository, questionService, examMapper, examGenerator);
        }

    }

    @Nested
    class GetById {

        @Test
        void shouldReturnExamDtoWhenFound() {
            final var user = UserTestData.createTestUserPippin();
            final var exam = ExamTestData.createExam(user);
            final var expectedDto = ExamTestData.createExamDto(exam);

            when(repository.findById(exam.getId())).thenReturn(Optional.of(exam));
            when(examMapper.toDto(exam)).thenReturn(expectedDto);

            final var result = service.getById(exam.getId());

            assertThat(result).isEqualTo(expectedDto);
        }

        @Test
        void shouldThrowEntityNotFoundExceptionWhenExamDoesNotExist() {
            final long nonExistentId = 666L;
            when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(nonExistentId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(String.valueOf(nonExistentId));
        }

        @Test
        void shouldCallMapperWithCorrectExam() {
            final var user = UserTestData.createTestUserPippin();
            final var exam = ExamTestData.createExam(user);
            when(repository.findById(exam.getId())).thenReturn(Optional.of(exam));
            when(examMapper.toDto(exam)).thenReturn(ExamTestData.createExamDto(exam));

            service.getById(exam.getId());

            verify(examMapper).toDto(exam);
        }

    }

    @Nested
    class GetAllByUserId {

        @Test
        void returnSummaries_inRepositoryOrder_whenUserHasExams() {
            final var userId = 44L;
            final var user = UserTestData.createTestUserPippin();
            final var older = ExamTestData.createExam(user).setId(1L);
            final var newer = ExamTestData.createExam(user).setId(2L);
            final var newerDto = ExamTestData.createExamSummaryDto(newer);
            final var olderDto = ExamTestData.createExamSummaryDto(older);
            when(userService.getById(userId)).thenReturn(user);
            final var pageable = PageRequest.of(0, 2);
            when(repository.findAllByUser_Id(userId, pageable)).thenReturn(new PageImpl<>(List.of(newer, older), pageable, 5));
            when(examMapper.toSummaryDto(newer)).thenReturn(newerDto);
            when(examMapper.toSummaryDto(older)).thenReturn(olderDto);

            final var result = service.getAllByUserId(userId, pageable);

            assertThat(result.getContent()).containsExactly(newerDto, olderDto);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getPageable()).isEqualTo(pageable);
        }

        @Test
        void returnEmptyList_whenUserHasNoExams() {
            final var userId = 44L;
            when(userService.getById(userId)).thenReturn(UserTestData.createTestUserPippin());
            final var pageable = PageRequest.of(0, 20);
            when(repository.findAllByUser_Id(userId, pageable)).thenReturn(Page.empty(pageable));

            final var result = service.getAllByUserId(userId, pageable);

            assertThat(result).isEmpty();
            verifyNoInteractions(examMapper);
        }

        @Test
        void throwEntityNotFound_whenUserDoesNotExist() {
            final var userId = 666L;
            final var expectedMessage = UserTestData.userNotFoundMessage(userId);
            when(userService.getById(userId)).thenThrow(new EntityNotFoundException(expectedMessage));

            assertThatThrownBy(() -> service.getAllByUserId(userId, PageRequest.of(0, 20)))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(expectedMessage);

            verifyNoInteractions(repository, examMapper);
        }

    }

}
