package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.repository.ExamRepository;
import pl.prawko.prawko_server.service.implementation.CategoryService;
import pl.prawko.prawko_server.service.implementation.ExamService;
import pl.prawko.prawko_server.service.implementation.QuestionService;
import pl.prawko.prawko_server.service.implementation.UserService;
import pl.prawko.prawko_server.test_data.TestDataFactory;

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

    @InjectMocks
    private ExamService service;

    private final TestDataFactory testDataFactory = new TestDataFactory();

    /*@Test
    void createExam_returnExam_whenUserAndCategoryFound() {
        final var user = testDataFactory.createTestUserPippin();
        final var category = CategoryTestData.CATEGORY_B;
        final var categoryName = category.getName();
        final var expected = testDataFactory.createExam(user);
        when(userService.getById(user.getId())).thenReturn(user);
        when(categoryService.findByName(categoryName)).thenReturn(category);
        when(questionService.getAllByTypeAndCategory(QuestionType.BASIC, categoryName))
                .thenReturn(testDataFactory.createThreeQuestions(QuestionType.BASIC));
        when(questionService.getAllByTypeAndCategory(QuestionType.SPECIAL, categoryName))
                .thenReturn(testDataFactory.createThreeQuestions(QuestionType.SPECIAL));

        final var result = service.createExam(user.getId(), categoryName);
        assertThat(result).hasValue(expected);
        verify(repository).save(result.get());
        verifyNoMoreInteractions(repository);
    }*/

}
