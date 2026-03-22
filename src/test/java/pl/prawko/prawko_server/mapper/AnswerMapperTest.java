package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.test_data.AnswerTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = AnswerMapperImpl.class)
class AnswerMapperTest {

    @Autowired
    private AnswerMapper mapper;

    @Test
    void toDto_correctlyMapAnswer() {
        final var given = QuestionTestData.createQuestion(QuestionType.SPECIAL)
                .getAnswers()
                .getFirst();
        final var expected = AnswerTestData.createAnswerDtoA();

        final var result = mapper.toDto(given);

        assertThat(result).isEqualTo(expected);
    }

}
