package pl.prawko.prawko_server.repository;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // a null parameter matches any value; exists keeps questions with several categories from being counted twice
    String MATCHING = """
            from Question q
            where (:type is null or q.type = :type)
            and (:category is null or exists (select c from q.categories c where c.name = :category))
            """;

    List<Question> findByTypeAndCategories_NameContains(final QuestionType type, final String category);

    @Query("select count(q) " + MATCHING)
    long countMatching(@Nullable QuestionType type, @Nullable String category);

    @Query("select q " + MATCHING + "order by q.id")
    List<Question> findMatching(@Nullable QuestionType type, @Nullable String category, Pageable pageable);

}
