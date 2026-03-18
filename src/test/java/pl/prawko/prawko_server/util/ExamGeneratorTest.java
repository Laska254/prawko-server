package pl.prawko.prawko_server.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.implementation.QuestionService;
import pl.prawko.prawko_server.test_data.CategoryTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExamGeneratorTest {

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private ExamGenerator examGenerator;

    private static final Category CATEGORY = CategoryTestData.CATEGORY_B;

    @Nested
    class Generate {

        @BeforeEach
        void setUp() {
            stubQuestions(QuestionType.BASIC);
            stubQuestions(QuestionType.SPECIAL);
        }

        @Test
        void shouldReturnCorrectTotalNumberOfQuestions() {
            final var questions = examGenerator.generate(CATEGORY);

            assertThat(questions).hasSize(32);
        }

        @Test
        void shouldReturnCorrectNumberOfBasicQuestions() {
            final var questions = examGenerator.generate(CATEGORY);

            final var basicCount = questions.stream().filter(q -> q.getType() == QuestionType.BASIC).count();
            assertThat(basicCount).isEqualTo(20);
        }

        @Test
        void shouldReturnCorrectNumberOfSpecialQuestions() {
            List<Question> result = examGenerator.generate(CATEGORY);

            long specialCount = result.stream().filter(q -> q.getType() == QuestionType.SPECIAL).count();
            assertThat(specialCount).isEqualTo(12);
        }

        @Test
        void shouldDelegateToQuestionServiceForBothTypes() {
            examGenerator.generate(CATEGORY);

            verify(questionService).getAllByTypeAndCategory(QuestionType.BASIC, CATEGORY.getName());
            verify(questionService).getAllByTypeAndCategory(QuestionType.SPECIAL, CATEGORY.getName());
        }

    }

    private void stubQuestions(final QuestionType type) {
        final var questions = type.getDistribution().entrySet().stream()
                .flatMap(e -> QuestionTestData.createQuestionsByTypeAndValue(type, e.getKey(), e.getValue()).stream())
                .toList();
        when(questionService.getAllByTypeAndCategory(type, CATEGORY.getName())).thenReturn(questions);
    }

}
