package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.test_data.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamMapperTest {

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private AnswerMapper answerMapper;

    @InjectMocks
    private ExamMapper examMapper;

    private final TestDataFactory factory = new TestDataFactory();

    @Test
    void toDto_correctlyMapsExam() {
        final var user = factory.createTestUserPippin().setId(1L);
        final var exam = factory.createExam(user);
        final var expectedDto = factory.createExamDto(exam);

        exam.getQuestions().forEach(q ->
                when(questionMapper.toDto(q)).thenReturn(expectedDto.questions().get(exam.getQuestions().indexOf(q))));

        final var result = examMapper.toDto(exam);

        assertThat(result).isEqualTo(expectedDto);
    }

}
