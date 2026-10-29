package pl.prawko.prawko_server.service;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.dto.ExamSummaryDto;
import pl.prawko.prawko_server.model.CategoryVariant;
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
     * @param categoryName the category from which to generate exam questions
     * @return the ID of the newly created exam
     */
    long createExam(long userId, CategoryVariant categoryName);

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
     * Retrieves a page of exams history of a specific user, optionally only of one category.
     * <p>
     * Returns exams as {@link ExamSummaryDto}, ordered according to the given {@link Pageable}.
     * </p>
     *
     * @param userId   the ID of the user whose exams should be retrieved
     * @param category the category of the exams, or {@code null} for all categories
     * @param pageable the pagination and sorting information
     * @return page of {@link ExamSummaryDto}, empty if the user has no matching exams
     */
    Page<ExamSummaryDto> getAllByUserId(long userId, @Nullable CategoryVariant category, Pageable pageable);

    /**
     * Saves the user's answer to a question of their active {@link Exam}.
     * <p>
     * The question is determined by the answer, so a previous answer to the same question is replaced.
     * Answers are accepted only until the exam expires, {@link Exam#DURATION} after its creation.
     * Concurrent changes to the same exam are detected by its version, so the later commit fails with
     * {@code OptimisticLockingFailureException} instead of overwriting the other one.
     * </p>
     *
     * @param examId   the ID of the exam
     * @param userId   the ID of the user answering, must be the owner of the exam
     * @param answerId the ID of the chosen answer, must belong to one of the exam's questions
     */
    void submitAnswer(long examId, long userId, long answerId);

    /**
     * Finishes the user's active {@link Exam}, so no more answers can be submitted.
     * <p>
     * The score is the sum of points of questions answered correctly; unanswered questions give no points.
     * Concurrent changes to the same exam are detected by its version, so the later commit fails with
     * {@code OptimisticLockingFailureException}.
     * </p>
     *
     * @param examId the ID of the exam
     * @param userId the ID of the user finishing the exam, must be the owner of the exam
     * @return {@link ExamDto} of the finished exam, revealing which answers are correct
     */
    ExamDto finishExam(long examId, long userId);

    /**
     * Finishes all active exams that have expired, scoring them as in
     * {@link #finishExam(long, long)}.
     * <p>
     * Runs periodically, every {@code exam.expiry-check-interval}. If one of the exams is modified concurrently,
     * none is finished, and they are picked up again by the next run.
     * </p>
     */
    void finishExpiredExams();

}
