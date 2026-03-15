package pl.prawko.prawko_server.test_data;

import pl.prawko.prawko_server.dto.AnswerDto;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.QuestionType;

import java.util.List;

public class AnswerTestData {

    private AnswerTestData() {
    }

    public static Answer answerA() {
        return new Answer()
                .setCorrect(false)
                .setTranslations(AnswerTranslationsTestData.variantA());
    }

    public static Answer answerB() {
        return new Answer()
                .setCorrect(true)
                .setTranslations(AnswerTranslationsTestData.variantB());
    }

    public static Answer answerC() {
        return new Answer()
                .setCorrect(false)
                .setTranslations(AnswerTranslationsTestData.variantC());
    }

    public static Answer yesAnswer() {
        return new Answer()
                .setCorrect(false);
    }

    public static Answer noAnswer() {
        return new Answer()
                .setCorrect(true);
    }

    public static AnswerDto createAnswerDtoA() {
        return new AnswerDto(
                0L,
                2143L,
                false,
                AnswerTranslationsTestData.createAnswerTranslationsDtos(AnswerVariant.A));
    }

    static List<Answer> createAnswers(final QuestionType type) {
        return switch (type) {
            case BASIC -> List.of(
                    AnswerTestData.noAnswer(),
                    AnswerTestData.yesAnswer()
            );
            case SPECIAL -> List.of(
                    AnswerTestData.answerA(),
                    AnswerTestData.answerB(),
                    AnswerTestData.answerC()
            );
        };
    }

}
