package pl.prawko.prawko_server.test_data;

import pl.prawko.prawko_server.dto.QuestionDto;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;

import java.util.List;
import java.util.stream.IntStream;

public class QuestionTestData {

    private QuestionTestData() {
    }

    public static Question createQuestion(final QuestionType type) {
        return switch (type) {
            case BASIC -> {
                final var translations = QuestionTranslationsTestData.create(QuestionType.BASIC);
                final var answers = AnswerTestData.createAnswers(type);
                final var question = new Question()
                        .setName("W9(2)")
                        .setId(110L)
                        .setMedia("AK_D11_45org.webm")
                        .setType(type)
                        .setPoints(3)
                        .setCategories(List.of(CategoryTestData.CATEGORY_A, CategoryTestData.CATEGORY_B))
                        .setTranslations(translations)
                        .setAnswers(answers);
                translations.forEach(translation -> translation.setQuestion(question));
                answers.forEach(answer -> answer.setQuestion(question));
                yield question;
            }
            case SPECIAL -> {
                final var translations = QuestionTranslationsTestData.create(QuestionType.SPECIAL);
                final var answers = AnswerTestData.createAnswers(type);
                final var question = new Question()
                        .setName("PD10(3)")
                        .setId(2143L)
                        .setMedia("R_101org.jpg")
                        .setType(type)
                        .setPoints(2)
                        .setCategories(List.of(CategoryTestData.CATEGORY_PT))
                        .setTranslations(translations)
                        .setAnswers(answers);
                translations.forEach(translation -> translation.setQuestion(question));
                answers.forEach(answer -> {
                    answer.setQuestion(question);
                    answer.getTranslations()
                            .forEach(translation -> translation.setAnswer(answer));
                });
                yield question;
            }
        };
    }

    public static QuestionDto createQuestionDto() {
        final var answers = AnswerTestData.createAnswersDtos();
        final var categories = List.of(CategoryTestData.CATEGORY_PT.getName());
        final var translations = QuestionTranslationsTestData.createDtos(QuestionType.SPECIAL);
        return new QuestionDto(
                2143L,
                "PD10(3)",
                answers,
                "R_101org.jpg",
                QuestionType.SPECIAL,
                2,
                categories,
                translations
        );
    }

    public static List<Question> createQuestionsByTypeAndValue(QuestionType type, int points, int count) {
        return IntStream.range(0, count)
                .mapToObj(question -> new Question().setType(type).setPoints(points))
                .toList();
    }

    static List<Question> createQuestions(final QuestionType type) {
        return switch (type) {
            case BASIC -> createBasicQuestions();
            case SPECIAL -> createSpecialQuestions();
        };
    }

    private static List<Question> createBasicQuestions() {
        return List.of(
                createSimpleQuestion(1L, QuestionType.BASIC, 1),
                createSimpleQuestion(2L, QuestionType.BASIC, 2),
                createSimpleQuestion(3L, QuestionType.BASIC, 3)
        );
    }

    private static List<Question> createSpecialQuestions() {
        return List.of(
                createSimpleQuestion(4L, QuestionType.SPECIAL, 1),
                createSimpleQuestion(5L, QuestionType.SPECIAL, 2),
                createSimpleQuestion(6L, QuestionType.SPECIAL, 3)
        );
    }

    private static Question createSimpleQuestion(final long id, final QuestionType type, final int points) {
        return new Question()
                .setType(type)
                .setCategories(List.of(CategoryTestData.CATEGORY_B))
                .setId(id)
                .setPoints(points);
    }

}
