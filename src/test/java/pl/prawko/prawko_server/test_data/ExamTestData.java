package pl.prawko.prawko_server.test_data;

import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.dto.ExamSummaryDto;
import pl.prawko.prawko_server.dto.QuestionDto;
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
import java.util.stream.Stream;

public class ExamTestData {

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
                        question.getMedia(),
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

    public static Exam expire(final Exam exam) {
        return exam.setExpires(Instant.now().minus(1, ChronoUnit.MINUTES));
    }

    public static String answerNotFoundMessage(final long answerId, final long examId) {
        return "Answer with '" + answerId + "' not found in exam '" + examId + "'.";
    }

}
