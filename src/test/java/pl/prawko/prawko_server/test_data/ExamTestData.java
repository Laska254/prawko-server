package pl.prawko.prawko_server.test_data;

import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.dto.QuestionDto;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.model.Exam;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.model.User;

import java.util.Collections;
import java.util.stream.Stream;

public class ExamTestData {

    private ExamTestData() {
    }

    public static Exam createExam(final User user) {
        final var exam = new Exam()
                .setUser(user)
                .setCategory(CategoryTestData.CATEGORY_B)
                .setQuestions(Stream.concat(
                                QuestionTestData.createQuestions(QuestionType.BASIC).stream(),
                                QuestionTestData.createQuestions(QuestionType.SPECIAL).stream())
                        .toList())
                .setScore(0)
                .setActive(true)
                .setUserAnswers(Collections.emptyList());
        user.getExams().add(exam);
        return exam;
    }

    public static ExamDto createExamDto(final Exam exam) {
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

}
