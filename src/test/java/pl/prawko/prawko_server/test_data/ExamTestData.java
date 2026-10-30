package pl.prawko.prawko_server.test_data;

import pl.prawko.prawko_server.dto.CategoryStatsDto;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.dto.ExamSummaryDto;
import pl.prawko.prawko_server.dto.QuestionDto;
import pl.prawko.prawko_server.dto.UserStatsDto;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.model.Exam;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.model.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class ExamTestData {

    // the maximum score, so counting an exam with it would change every stat
    private static final int MAX_SCORE = 74;

    public static final UserStatsDto EMPTY_STATS = new UserStatsDto(0, 0, null, null, List.of());

    // matches createExamsForStats
    public static final UserStatsDto STATS = new UserStatsDto(3, 2, 66.0, 70, List.of(
            new CategoryStatsDto(CategoryTestData.CATEGORY_A.getName(), 1, 1, Exam.PASSING_SCORE, Exam.PASSING_SCORE),
            new CategoryStatsDto(CategoryTestData.CATEGORY_B.getName(), 2, 1, 65.0, 70)));

    private ExamTestData() {
    }

    public static Exam createExam(final User user) {
        // the database stores microseconds, so truncate to compare persisted exams with the original
        final var now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        final var exam = new Exam()
                .setUser(user)
                .setCategory(CategoryTestData.CATEGORY_B)
                .setCreated(now)
                .setExpires(now.plus(Exam.DURATION))
                .setQuestions(Stream.concat(
                                QuestionTestData.createQuestions(QuestionType.BASIC).stream(),
                                QuestionTestData.createQuestions(QuestionType.SPECIAL).stream())
                        .toList())
                .setScore(0)
                .setActive(true)
                .setUserAnswers(new ArrayList<>());
        user.getExams().add(exam);
        return exam;
    }

    public static Exam createExamWithoutQuestions(final User user) {
        return createExam(user).setQuestions(Collections.emptyList());
    }

    public static ExamSummaryDto createExamSummaryDto(final Exam exam) {
        return new ExamSummaryDto(
                exam.getId(),
                exam.getCategory().getName(),
                exam.getCreated(),
                exam.getUpdated(),
                exam.getExpires(),
                exam.getScore(),
                exam.isActive());
    }

    public static ExamDto createExamDto(final Exam exam) {
        final var questions = exam.getQuestions().stream()
                .sorted(Comparator.comparing(Question::getId))
                .map(question -> new QuestionDto(
                        question.getId(),
                        question.getName(),
                        Collections.emptyList(),
                        // the exam fixtures (createSimpleQuestion) set no media
                        null,
                        null,
                        question.getType(),
                        question.getPoints(),
                        question.getCategories().stream().map(Category::getName).toList(),
                        Collections.emptyList()))
                .toList();

        return new ExamDto(
                exam.getId(),
                exam.getUser().getId(),
                exam.getCategory().getName(),
                exam.getCreated(),
                exam.getUpdated(),
                exam.getExpires(),
                questions,
                Collections.emptyList(),
                exam.getScore(),
                exam.isActive());
    }

    public static String examNotFoundMessage(final long id) {
        return "Exam with '" + id + "' not found.";
    }

    public static String examFinishedMessage(final long id) {
        return "Exam with '" + id + "' is already finished.";
    }

    public static String examExpiredMessage(final long id) {
        return "Time for exam with '" + id + "' has expired.";
    }

    public static Exam finish(final Exam exam, final int score) {
        return exam
                .setActive(false)
                .setScore(score);
    }

    // finished exams in two categories, passed (one exactly at the threshold) and failed, plus an active one
    public static List<Exam> createExamsForStats(final User user) {
        return List.of(
                finish(createExamWithoutQuestions(user), 70),
                finish(createExamWithoutQuestions(user), 60),
                finish(createExamWithoutQuestions(user)
                        .setCategory(CategoryTestData.CATEGORY_A), Exam.PASSING_SCORE),
                createExamWithoutQuestions(user)
                        .setScore(MAX_SCORE));
    }

    // an exam of another user that must not count towards createExamsForStats
    public static Exam createFinishedExamWithMaxScore(final User user) {
        return finish(createExamWithoutQuestions(user), MAX_SCORE);
    }

    public static Exam expire(final Exam exam) {
        return exam.setExpires(Instant.now().minus(1, ChronoUnit.MINUTES));
    }

    public static String answerNotFoundMessage(final long answerId, final long examId) {
        return "Answer with '" + answerId + "' not found in exam '" + examId + "'.";
    }

}
