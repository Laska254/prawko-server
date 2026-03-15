package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import pl.prawko.prawko_server.mapper.AnswerMapper;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.Language;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.repository.QuestionRepository;
import pl.prawko.prawko_server.service.implementation.CategoryService;
import pl.prawko.prawko_server.service.implementation.LanguageService;
import pl.prawko.prawko_server.service.implementation.QuestionService;
import pl.prawko.prawko_server.test_data.CategoryTestData;
import pl.prawko.prawko_server.test_data.LanguageTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;

import java.io.IOException;
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

    @Mock
    private CategoryService categoryService;

    @Mock
    private LanguageService languageService;

    private final List<Language> languages = LanguageTestData.ALL;
    private QuestionService questionService;

    @BeforeEach
    void setUp() {
        final var answerMapper = new AnswerMapper(languageService);
        final var questionMapper = new QuestionMapper(categoryService, languageService, answerMapper);
        questionService = new QuestionService(repository, questionMapper);
    }

    @Test
    void mapCSVFile_correctly() throws IOException {
        final ClassPathResource resource = new ClassPathResource("test_question.csv");
        final var inputStream = resource.getInputStream();
        final var file = new MockMultipartFile("file", "test_question.csv", "text/csv", inputStream);
        when(categoryService.findAllFromString("A,B")).thenReturn(List.of(CategoryTestData.CATEGORY_A, CategoryTestData.CATEGORY_B));
        when(categoryService.findAllFromString("PT")).thenReturn(List.of(CategoryTestData.CATEGORY_PT));
        when(languageService.findAll()).thenReturn(languages);
        final var expected = List.of(QuestionTestData.createQuestion(QuestionType.BASIC), QuestionTestData.createQuestion(QuestionType.SPECIAL));

        final var result = questionService.parseFileToQuestions(file);

        assertThat(resource)
                .satisfies(res -> {
                    assertThat(res.exists()).isTrue();
                    assertThat(res.isReadable()).isTrue();
                });
        assertThat(result).containsExactlyInAnyOrder(expected.toArray(new Question[0]));
    }

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
