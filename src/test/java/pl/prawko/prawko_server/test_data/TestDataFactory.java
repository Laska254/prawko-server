package pl.prawko.prawko_server.test_data;

import pl.prawko.prawko_server.dto.AnswerDto;
import pl.prawko.prawko_server.dto.AnswerTranslationDto;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.dto.QuestionDto;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.AnswerTranslation;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.model.Exam;
import pl.prawko.prawko_server.model.Language;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCSV;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.model.User;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static pl.prawko.prawko_server.test_data.LanguageTestData.DE;
import static pl.prawko.prawko_server.test_data.LanguageTestData.EN;
import static pl.prawko.prawko_server.test_data.LanguageTestData.PL;

public class TestDataFactory {

    private final Question question = createQuestion(QuestionType.SPECIAL);

    public Question createQuestion(final QuestionType type) {
        return switch (type) {
            case BASIC -> {
                final var translations = QuestionTranslationsTestData.create(QuestionType.BASIC);
                final var answers = createAnswers(type);
                final var question = new Question()
                        .setName("W9(2)")
                        .setId(110)
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
                final var answers = createAnswers(type);
                final var question = new Question()
                        .setName("PD10(3)")
                        .setId(2143)
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

    public List<Question> createThreeQuestions(final QuestionType type) {
        return switch (type) {
            case BASIC -> createBasicQuestions();
            case SPECIAL -> createSpecialQuestions();
        };
    }

    public QuestionCSV createSpecialQuestionCSV() {
        final var question = createQuestion(QuestionType.SPECIAL);
        final var answerTranslationsA = createQuestionTranslations(AnswerVariant.A);
        final var answerTranslationsB = createQuestionTranslations(AnswerVariant.B);
        final var answerTranslationsC = createQuestionTranslations(AnswerVariant.C);
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

    public QuestionCSV createBasicQuestionCSV() {
        final var question = createQuestion(QuestionType.BASIC);
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


    private List<Question> createBasicQuestions() {
        return List.of(
                createSimpleQuestion(1L, QuestionType.BASIC, 1),
                createSimpleQuestion(2L, QuestionType.BASIC, 2),
                createSimpleQuestion(3L, QuestionType.BASIC, 3)
        );
    }

    private List<Question> createSpecialQuestions() {
        return List.of(
                createSimpleQuestion(4L, QuestionType.SPECIAL, 1),
                createSimpleQuestion(5L, QuestionType.SPECIAL, 2),
                createSimpleQuestion(6L, QuestionType.SPECIAL, 3)
        );
    }

    private Question createSimpleQuestion(final long id, final QuestionType type, final int points) {
        return new Question()
                .setType(type)
                .setCategories(List.of(CategoryTestData.CATEGORY_B))
                .setId(id)
                .setPoints(points);
    }

    public Exam createExam(final User user) {
        final var exam = new Exam()
                .setUser(user)
                .setCategory(CategoryTestData.CATEGORY_B)
                .setQuestions(Stream.concat(
                                createThreeQuestions(QuestionType.BASIC).stream(),
                                createThreeQuestions(QuestionType.SPECIAL).stream())
                        .toList())
                .setScore(0)
                .setActive(true)
                .setUserAnswers(Collections.emptyList());
        user.getExams().add(exam);
        return exam;
    }

    public ExamDto createExamDto(final Exam exam) {
        final var questions = exam.getQuestions().stream()
                .map(question -> new QuestionDto(
                        question.getId(),
                        question.getName(),
                        Collections.emptyList(),
                        question.getMedia(),
                        question.getType(),
                        question.getPoints(),
                        question.getCategories().stream().map(Category::getName).toList(),
                        Collections.emptyList()))
                .toList();

        return new ExamDto(
                exam.getId(),
                exam.getUser().getId(),
                exam.getCreated(),
                exam.getUpdated(),
                questions,
                Collections.emptyList(),
                exam.getScore(),
                exam.isActive());
    }

    public Answer createAnswer(final AnswerVariant variant) {
        return switch (variant) {
            case A -> createAnswerA();
            case B -> createAnswerB();
            case C -> createAnswerC();
            case YES -> createAnswerY();
            case NO -> createAnswerN();
        };
    }

    public List<Answer> createAnswers(final QuestionType type) {
        return switch (type) {
            case BASIC -> List.of(
                    createAnswer(AnswerVariant.NO),
                    createAnswer(AnswerVariant.YES)
            );
            case SPECIAL -> List.of(
                    createAnswer(AnswerVariant.A),
                    createAnswer(AnswerVariant.B),
                    createAnswer(AnswerVariant.C)
            );
        };
    }

    public AnswerDto createAnswerDtoA() {
        return new AnswerDto(
                0L,
                question.getId(),
                false,
                createAnswerTranslationsDtos(AnswerVariant.A));
    }

    public List<AnswerTranslation> createQuestionTranslations(final AnswerVariant variant) {
        return switch (variant) {
            case A -> createAnswerTranslationsA();
            case B -> createAnswerTranslationsB();
            case C -> createAnswerTranslationsC();
            default -> throw new IllegalStateException("Unexpected value: " + variant);
        };
    }

    public List<AnswerTranslationDto> createAnswerTranslationsDtos(final AnswerVariant variant) {
        return createQuestionTranslations(variant).stream()
                .map(translation -> new AnswerTranslationDto(
                        translation.getContent(),
                        translation.getLanguage().getCode()))
                .toList();
    }

    private AnswerTranslation createAnswerTranslation(final Language language, final String content) {
        return new AnswerTranslation()
                .setLanguage(language)
                .setContent(content);
    }

    private List<AnswerTranslation> createAnswerTranslationsA() {
        return List.of(
                createAnswerTranslation(PL, "Co 60 minut."),
                createAnswerTranslation(EN, "Every 60 minutes."),
                createAnswerTranslation(DE, "jede 60 Minuten")
        );
    }

    private List<AnswerTranslation> createAnswerTranslationsB() {
        return List.of(
                createAnswerTranslation(PL, "Co 30 minut."),
                createAnswerTranslation(EN, "Every 30 minutes."),
                createAnswerTranslation(DE, "jede 30 Minuten")
        );
    }

    private List<AnswerTranslation> createAnswerTranslationsC() {
        return List.of(
                createAnswerTranslation(PL, "Co 15 minut."),
                createAnswerTranslation(EN, "Every 15 minutes."),
                createAnswerTranslation(DE, "jede 15 Minuten")
        );
    }

    private Answer createAnswerA() {
        return new Answer()
                .setQuestion(question)
                .setCorrect(false)
                .setTranslations(createAnswerTranslationsA());
    }

    private Answer createAnswerB() {
        return new Answer()
                .setQuestion(question)
                .setCorrect(true)
                .setTranslations(createAnswerTranslationsB());
    }

    private Answer createAnswerC() {
        return new Answer()
                .setQuestion(question)
                .setCorrect(false)
                .setTranslations(createAnswerTranslationsC());
    }

    private Answer createAnswerY() {
        return new Answer().setCorrect(false);
    }

    private Answer createAnswerN() {
        return new Answer().setCorrect(true);
    }

}
