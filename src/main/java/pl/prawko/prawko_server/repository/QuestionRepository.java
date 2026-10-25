package pl.prawko.prawko_server.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByTypeAndCategories_NameContains(final QuestionType type, final String category);

    long countByTypeInAndCategories_Name(Collection<QuestionType> types, String category);

    List<Question> findByTypeInAndCategories_Name(Collection<QuestionType> types, String category, Pageable pageable);

}
