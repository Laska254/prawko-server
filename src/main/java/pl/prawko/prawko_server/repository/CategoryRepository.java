package pl.prawko.prawko_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.prawko.prawko_server.model.Category;

import java.util.Optional;

/**
 * Repository for {@link Category} entities.
 * <p>
 * Provides standard CRUD operations through {@link JpaRepository} and custom methods.
 * </p>
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Retrieves a category by its name.
     *
     * @param name the name of the category to find
     * @return an {@link Optional} of {@link Category} if found, otherwise empty
     */
    Optional<Category> findByName(final String name);

}
