package pl.prawko.prawko_server.service;

import org.jspecify.annotations.Nullable;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.model.CategoryVariant;

import java.util.List;

/**
 * Service interface for managing and retrieving {@link Category} entities.
 * <p>
 * Provides operations for finding categories by name and for parsing multiple categories from a comma-separated string.
 * </p>
 */
public interface ICategoryService {

    /**
     * Finds a {@link Category} entity by its name.
     *
     * @param name name of the category to retrieve
     * @return found {@link Category}
     */
    Category findByName(CategoryVariant name);

    /**
     * Parses a comma-separated string of category names and retrieves all matching {@link Category} entities from the repository.
     * <p>
     * Every name must be a stored {@link CategoryVariant}, matched exactly. An empty name (an empty or {@code null}
     * input, or two adjacent commas) is unknown too. All unknown names are reported together.
     * </p>
     *
     * @param input a comma-separated string of category names
     * @return the matching {@link Category} entities, in input order
     */
    List<Category> findAllFromString(@Nullable String input);

}
