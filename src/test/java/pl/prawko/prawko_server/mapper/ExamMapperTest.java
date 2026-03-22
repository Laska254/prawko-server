package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.Exam;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(classes = ExamMapperImpl.class)
class ExamMapperTest {

    @Autowired
    private ExamMapper examMapper;

    @MockitoBean
    private QuestionMapper questionMapper;

    @MockitoBean
    private AnswerMapper answerMapper;

    @Test
    void toDto_shouldDelegate_ToQuestionAndAnswerMappersAndMapAllFields() {
        var now = LocalDateTime.now();
        var question = new Question().setId(10L);
        var answer = new Answer().setId(20L);
        var expected = new Exam()
                .setId(1L)
                .setUser(new User().setId(42L))
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
