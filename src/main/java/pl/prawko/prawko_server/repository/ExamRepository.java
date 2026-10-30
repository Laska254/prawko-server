package pl.prawko.prawko_server.repository;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.prawko.prawko_server.dto.CategoryStatsDto;
import pl.prawko.prawko_server.model.CategoryVariant;
import pl.prawko.prawko_server.model.Exam;

import java.time.Instant;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    /**
     * Retrieves a page of exams of a user.
     *
     * @param userId   the ID of the user owning the exams
     * @param pageable the pagination and sorting information
     * @return a page of user's exams
     */
    Page<Exam> findAllByUser_Id(final long userId, final Pageable pageable);

    /**
     * Retrieves a page of exams of a user in one category.
     *
     * @param userId   the ID of the user owning the exams
     * @param category the category of the exams
     * @param pageable the pagination and sorting information
     * @return a page of user's exams in the category
     */
    Page<Exam> findAllByUser_IdAndCategory_Name(final long userId, final CategoryVariant category, final Pageable pageable);
    
    List<Exam> findAllByActiveTrueAndExpiresBefore(final Instant time);

    /**
     * Aggregates the finished exams of a user.
     *
     * @param userId       the ID of the user owning the exams
     * @param passingScore the lowest score of a passed exam
     * @return the totals; without finished exams the counts are {@code 0}, the scores {@code null}
     */
    @Query("""
            SELECT COUNT(e) AS examsTaken,
                   COALESCE(SUM(CASE WHEN e.score >= :passingScore THEN 1 ELSE 0 END), 0) AS examsPassed,
                   AVG(e.score) AS averageScore,
                   MAX(e.score) AS bestScore
            FROM Exam e
            WHERE e.user.id = :userId AND e.active = false
            """)
    ExamTotals findTotalsByUserId(final long userId, final int passingScore);

    /**
     * Aggregates the finished exams of a user per category.
     *
     * @param userId       the ID of the user owning the exams
     * @param passingScore the lowest score of a passed exam
     * @return statistics of categories with finished exams, ordered by category name
     */
    @Query("""
            SELECT new pl.prawko.prawko_server.dto.CategoryStatsDto(
                       e.category.name,
                       COUNT(e),
                       SUM(CASE WHEN e.score >= :passingScore THEN 1 ELSE 0 END),
                       AVG(e.score),
                       MAX(e.score))
            FROM Exam e
            WHERE e.user.id = :userId AND e.active = false
            GROUP BY e.category.name
            ORDER BY e.category.name
            """)
    List<CategoryStatsDto> findCategoryStatsByUserId(final long userId, final int passingScore);

    /**
     * Projection of {@link #findTotalsByUserId(long, int)}.
     */
    interface ExamTotals {

        long getExamsTaken();

        long getExamsPassed();

        @Nullable
        Double getAverageScore();

        @Nullable
        Integer getBestScore();

    }

}
