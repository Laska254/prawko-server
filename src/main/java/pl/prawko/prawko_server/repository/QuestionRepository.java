package pl.prawko.prawko_server.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.prawko.prawko_server.model.CategoryVariant;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByTypeAndCategories_Name(final QuestionType type, final CategoryVariant category);

    List<Question> findByTypeAndCategories_Name(QuestionType type, CategoryVariant category, Pageable pageable);

    long countByTypeAndCategories_Name(QuestionType type, CategoryVariant category);

}
