package pl.prawko.prawko_server.service.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.repository.CategoryRepository;
import pl.prawko.prawko_server.service.ICategoryService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link ICategoryService} that retrieves {@link Category} entities using a {@link CategoryRepository}.
 */
@Service
public class CategoryService implements ICategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    @NonNull
    private final CategoryRepository repository;

    /**
     * Constructs a {code CategoryService} with the given {@link CategoryRepository}.
     *
     * @param repository the category repository used for data access
     */
    public CategoryService(@NonNull final CategoryRepository repository) {
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     *
     * @throws RuntimeException when not found
     */
    @Override
    public Optional<Category> findByName(@NonNull final String name) {
        log.info("Fetching for category '{}'", name);
        return repository.findByName(name);
    }

    @Override
    public List<Category> findAllFromString(@NonNull final String input) {
        log.info("Fetching categories from '{}'", input);
        return Arrays.stream(input.split(","))
                .map(repository::findByName)
                .flatMap(Optional::stream)
                .toList();
    }

}
