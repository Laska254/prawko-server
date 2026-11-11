package pl.prawko.prawko_server.testdata;

import pl.prawko.prawko_server.dto.response.AnswerDto;
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
                .setCorrect(false)
                .setTranslations(List.of());
    }

    public static Answer noAnswer() {
        return new Answer()
                .setCorrect(true)
                .setTranslations(List.of());
    }

    public static List<AnswerDto> createAnswerDtos(final List<Answer> savedAnswers, final QuestionType type) {
        return switch (type) {
            case BASIC -> savedAnswers.stream()
                    .map(AnswerTestData::toBasicDto)
                    .toList();
            case SPECIAL -> savedAnswers.stream()
                    .map(AnswerTestData::toSpecialDto)
                    .toList();
        };
    }

    public static List<Answer> createAnswers(final QuestionType type) {
        return switch (type) {
            case BASIC -> List.of(
                    AnswerTestData.yesAnswer(),
                    AnswerTestData.noAnswer()
            );
            case SPECIAL -> List.of(
                    AnswerTestData.answerA(),
                    AnswerTestData.answerB(),
                    AnswerTestData.answerC()
            );
        };
    }

    private static AnswerDto toBasicDto(final Answer answer) {
        return new AnswerDto(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.isCorrect(),
                Collections.emptyList());
    }

    private static AnswerDto toSpecialDto(final Answer answer) {
        return new AnswerDto(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.isCorrect(),
                AnswerTranslationsTestData.createAnswerTranslationsDtos(answer.getTranslations()));
    }

}
