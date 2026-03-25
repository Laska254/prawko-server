package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.Exam;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExamMapperTest {

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private AnswerMapper answerMapper;

    @InjectMocks
    private ExamMapperImpl examMapper;

    @Test
    void toDto_shouldDelegate_ToQuestionAndAnswerMappersAndMapAllFields() {
        final var now = LocalDateTime.now();
        final var question = new Question().setId(10L);
        final var answer = new Answer().setId(20L);
        final var user = new User().setId(42L);
        final var expected = new Exam()
                .setId(1L)
                .setUser(user)
                .setActive(true)
                .setScore(67)
                .setCreated(now)
                .setUpdated(now)
                .setQuestions(List.of(question))
                .setUserAnswers(List.of(answer));

        final var result = examMapper.toDto(expected);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo(42L);
        assertThat(result.active()).isTrue();
        assertThat(result.score()).isEqualTo(67);
        assertThat(result.created()).isEqualTo(now);
        assertThat(result.updated()).isEqualTo(now);
        verify(questionMapper).toDto(question);
        verify(answerMapper).toDto(answer);
    }

}
