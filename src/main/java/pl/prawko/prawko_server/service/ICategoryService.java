package pl.prawko.prawko_server.service;

import pl.prawko.prawko_server.model.Category;

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
    Category findByName(String name);

    /**
     * Parses a comma-separated string of category names and retrieves all matching {@link Category} entities from the repository.
     * <p>
     * Categories that are not found in the repository will be ignored and not included in the resulting list.
     * </p>
     *
     * @param input a comma-separated string of category names
     * @return a list of matching {@link Category} entities
     */
    List<Category> findAllFromString(String input);

}
