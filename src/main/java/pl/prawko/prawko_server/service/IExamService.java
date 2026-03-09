package pl.prawko.prawko_server.service;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.model.Exam;

/**
 * Interface to manage {@link Exam} entities.
 */
public interface IExamService {

    /**
     * Generates a new {@code exam} for specific {@code user} using category name.
     *
     * @param userId       owner of exam
     * @param categoryName category used to construct an exam
     * @return a correctly generated {@code exam}
     */
    long createExam(long userId, @NonNull String categoryName);

    /**
     * Finds a {@link Exam} entity by its id and map it to {@link ExamDto}.
     *
     * @param examId id of exam to look for
     * @return mapped {@code exam}
     */
    @Nullable
    ExamDto getById(long examId);

}
