package pl.prawko.prawko_server.service.implementation;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.repository.CategoryRepository;
import pl.prawko.prawko_server.service.ICategoryService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link ICategoryService} that manages and retrieves {@link Category} entities.
 */
@Service
public class CategoryService implements ICategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository repository;

    public CategoryService(final CategoryRepository repository) {
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException if category with the given name have not been found
     */
    @Override
    public Category findByName(final String name) {
        log.info("Fetching for category '{}'", name);
        return repository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Category '" + name + "' not found."));
    }

    @Override
    public List<Category> findAllFromString(final String input) {
        log.info("Fetching categories from '{}'", input);
        return Arrays.stream(input.split(","))
                .map(repository::findByName)
                .flatMap(Optional::stream)
                .toList();
    }

}
