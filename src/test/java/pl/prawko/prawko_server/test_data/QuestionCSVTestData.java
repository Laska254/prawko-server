package pl.prawko.prawko_server.test_data;

import pl.prawko.prawko_server.model.QuestionCSV;
import pl.prawko.prawko_server.model.QuestionType;

public class QuestionCSVTestData {

    private QuestionCSVTestData() {
    }

    public static QuestionCSV createSpecialQuestionCSV() {
        final var question = QuestionTestData.createQuestion(QuestionType.SPECIAL);
        final var answerTranslationsA = AnswerTranslationsTestData.variantA();
        final var answerTranslationsB = AnswerTranslationsTestData.variantB();
        final var answerTranslationsC = AnswerTranslationsTestData.variantC();
        return new QuestionCSV(
                "PD10(3)",
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

    public static QuestionCSV createBasicQuestionCSV() {
        final var question = QuestionTestData.createQuestion(QuestionType.BASIC);
        return new QuestionCSV(
                "W9(2)",
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
                'N',
                "AK_D11_45org.wmv",
                "PODSTAWOWY",
                3,
                "A,B"
        );
    }

}
