package pl.prawko.prawko_server.testdata;

import pl.prawko.prawko_server.model.QuestionCsv;
import pl.prawko.prawko_server.model.QuestionType;

public class QuestionCsvTestData {

    public static final String CSV_FILE = "test_question.csv";
    public static final String MALFORMED_CSV_FILE = "malformed_question.csv";
    public static final String MALFORMED_CSV_MESSAGE = "Invalid CSV file at row 2 in column 'Liczba punktów': "
            + "Cannot deserialize value of type `int` from String \"three\": not a valid `int` value";
    public static final String UNKNOWN_CATEGORY_CSV_FILE = "unknown_category_question.csv";
    public static final String ALLOWED_CATEGORIES = "Allowed: A, A1, A2, AM, B, B1, C, C1, D, D1, T, PT.";
    public static final String UNKNOWN_CATEGORY_CSV_MESSAGE = "Invalid CSV file: row 2: unknown category 'Z'; "
            + "row 3: unknown category ''; row 3: unknown category 'BB'. " + ALLOWED_CATEGORIES;

    private QuestionCsvTestData() {
    }

    public static QuestionCsv createQuestionCsv(final QuestionType type) {
        return switch (type) {
            case BASIC -> createBasicQuestionCsv();
            case SPECIAL -> createSpecialQuestionCsv();
        };
    }

    public static QuestionCsv createSpecialQuestionCsv() {
        final var question = QuestionTestData.createQuestion(QuestionType.SPECIAL);
        final var answerTranslationsA = AnswerTranslationsTestData.variantA();
        final var answerTranslationsB = AnswerTranslationsTestData.variantB();
        final var answerTranslationsC = AnswerTranslationsTestData.variantC();
        return new QuestionCsv(
                2143,
                question.getTranslations().getFirst().getContent(),
                answerTranslationsA.getFirst().getContent(),
                answerTranslationsB.getFirst().getContent(),
                answerTranslationsC.getFirst().getContent(),
                question.getTranslations().get(1).getContent(),
                answerTranslationsA.get(1).getContent(),
                answerTranslationsB.get(1).getContent(),
                answerTranslationsC.get(1).getContent(),
                question.getTranslations().get(2).getContent(),
                answerTranslationsA.get(2).getContent(),
                answerTranslationsB.get(2).getContent(),
                answerTranslationsC.get(2).getContent(),
                'B',
                "R_101org.jpg",
                "SPECJALISTYCZNY",
                2,
                "PT");
    }

    public static QuestionCsv createBasicQuestionCsv() {
        return createBasicQuestionCsv('N');
    }

    public static QuestionCsv createBasicQuestionCsv(final char correctAnswer) {
        final var question = QuestionTestData.createQuestion(QuestionType.BASIC);
        return new QuestionCsv(
                110,
                question.getTranslations().getFirst().getContent(),
                "",
                "",
                "",
                question.getTranslations().get(1).getContent(),
                "",
                "",
                "",
                question.getTranslations().getLast().getContent(),
                "",
                "",
                "",
                correctAnswer,
                "AK_D11_45org.wmv",
                "PODSTAWOWY",
                3,
                "A,B"
        );
    }

}
