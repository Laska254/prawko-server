package pl.prawko.prawko_server.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
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

}
