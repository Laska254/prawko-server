package pl.prawko.prawko_server.test_data;

import pl.prawko.prawko_server.dto.AnswerDto;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.QuestionType;

import java.util.Collections;
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

    static List<AnswerDto> createAnswersDtos(final QuestionType type) {
        return switch (type) {
            case BASIC -> createAnswerDtos_Basic();
            case SPECIAL -> createAnswerDtos_Special();
        };
    }

    private static List<AnswerDto> createAnswerDtos_Basic() {
        return List.of(
                new AnswerDto(4L, 110L, false, Collections.emptyList()),
                new AnswerDto(5L, 110L, true, Collections.emptyList())
        );
    }

    private static List<AnswerDto> createAnswerDtos_Special() {
        return List.of(
                new AnswerDto(1L, 2143L, false, AnswerTranslationsTestData.createAnswerTranslationsDtos(AnswerVariant.A)),
                new AnswerDto(2L, 2143L, true, AnswerTranslationsTestData.createAnswerTranslationsDtos(AnswerVariant.B)),
                new AnswerDto(3L, 2143L, false, AnswerTranslationsTestData.createAnswerTranslationsDtos(AnswerVariant.C))
        );
    }

    public static List<Answer> createAnswers(final QuestionType type) {
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
