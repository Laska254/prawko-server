package pl.prawko.prawko_server.service;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.dto.ExamSummaryDto;
import pl.prawko.prawko_server.model.Exam;

/**
 * Service interface for managing {@link Exam} entities.
 * <p>
 * Provides operations for creating exams for users based on selected categories,
 * and retrieving exam details in DTO format.
 * </p>
 */
public interface IExamService {

    /**
     * Generates a new {@link Exam} for a specific user with questions from a given category.
     * <p>
     * The exam is initialized with questions randomly selected from the specified category
     * and is marked as active with a score of 0.
     * </p>
     *
     * @param userId       the ID of the user who owns the exam
     * @param categoryName the name of the category from which to generate exam questions
     * @return the ID of the newly created exam
     */
    long createExam(long userId, String categoryName);

    /**
     * Retrieves an {@link Exam} by its ID and maps it to {@link ExamDto}.
     * <p>
     * Returns exam details including user answers and score information in a data transfer object format.
     * Which answers are correct is revealed only once the exam is no longer active.
     * </p>
     *
     * @param examId the ID of the exam to retrieve
     * @return {@link ExamDto} containing exam details, or {@code null} if not found
     */
    @Nullable
    ExamDto getById(long examId);

    /**
     * Retrieves a page of exams history of a specific user.
     * <p>
     * Returns exams as {@link ExamSummaryDto}, ordered according to the given {@link Pageable}.
     * </p>
     *
     * @param userId   the ID of the user whose exams should be retrieved
     * @param pageable the pagination and sorting information
     * @return page of {@link ExamSummaryDto}, empty if the user has no exams
     */
    Page<ExamSummaryDto> getAllByUserId(long userId, Pageable pageable);

    /**
     * Saves the user's answer to a question of their active {@link Exam}.
     * <p>
     * The question is determined by the answer, so a previous answer to the same question is replaced.
     * </p>
     *
     * @param examId   the ID of the exam
     * @param userId   the ID of the user answering, must be the owner of the exam
     * @param answerId the ID of the chosen answer, must belong to one of the exam's questions
     */
    void submitAnswer(long examId, long userId, long answerId);

}
