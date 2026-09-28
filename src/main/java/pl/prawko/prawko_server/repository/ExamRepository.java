package pl.prawko.prawko_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.prawko.prawko_server.model.Exam;

import java.util.List;

/**
 * Repository for {@link Exam} entities.
 * <p>
 * Provides standard CRUD operations through {@link JpaRepository}.
 */
@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    /**
     * Retrieves all exams of a user, starting from the newest one.
     *
     * @param userId the ID of the user owning the exams
     * @return list of user's exams ordered by creation time descending
     */
    List<Exam> findAllByUser_IdOrderByCreatedDescIdDesc(final long userId);

}
