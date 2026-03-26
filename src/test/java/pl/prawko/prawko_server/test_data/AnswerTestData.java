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

    public static List<AnswerDto> createAnswerDtos(final List<Answer> savedAnswers, final QuestionType type) {
        return switch (type) {
            case BASIC -> savedAnswers.stream()
                    .map(AnswerTestData::toDto_BASIC)
                    .toList();
            case SPECIAL -> savedAnswers.stream()
                    .map(AnswerTestData::toDto_SPECIAL)
                    .toList();
        };
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

    private static AnswerDto toDto_BASIC(Answer answer) {
        return new AnswerDto(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.isCorrect(),
                Collections.emptyList());
    }

    private static AnswerDto toDto_SPECIAL(Answer answer) {
        return new AnswerDto(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.isCorrect(),
                AnswerTranslationsTestData.createAnswerTranslationsDtos(answer.getTranslations()));
    }

}
