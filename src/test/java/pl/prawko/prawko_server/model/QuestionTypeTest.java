package pl.prawko.prawko_server.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionTypeTest {

    @ParameterizedTest
    @CsvSource({"T, BASIC", "N, BASIC", "A, SPECIAL", "B, SPECIAL", "C, SPECIAL"})
    void ofCorrectAnswer_returnType_whenLabelIsKnown(final char label, final QuestionType expected) {
        assertThat(QuestionType.ofCorrectAnswer(label)).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(chars = {'D', 't', 'b', ' '})
    void ofCorrectAnswer_returnEmpty_whenLabelIsUnknown(final char label) {
        assertThat(QuestionType.ofCorrectAnswer(label)).isEmpty();
    }

}
