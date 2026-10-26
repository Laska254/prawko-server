package pl.prawko.prawko_server.service.implementation;

import jakarta.persistence.EntityNotFoundException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.dto.QuestionDto;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.CategoryVariant;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.repository.QuestionRepository;
import pl.prawko.prawko_server.service.IQuestionService;
import pl.prawko.prawko_server.util.CSVParser;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementation of {@link IQuestionService} that manages {@link Question} entities and parsing csv file to {@code question} entities.
 */
@Service
public class QuestionService implements IQuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private final QuestionRepository repository;
    private final QuestionMapper mapper;
    private final CSVParser parser;

    public QuestionService(final QuestionRepository repository,
                           final QuestionMapper mapper,
                           final CSVParser parser) {
        this.repository = repository;
        this.mapper = mapper;
        this.parser = parser;
    }

    /**
     * {@inheritDoc}
     *
     * @throws MultipartException if the file is not of type "text/csv"
     * @throws RuntimeException   if there is an error reading or parsing the CSV file
     */
    @Override
    public List<Question> parseFileToQuestions(final MultipartFile file) {
        log.info("Attempting to parse file '{}'", file.getOriginalFilename());
        final var questions = parser.parse(file);
        log.info("Saving {} question(s)", questions.size());
        repository.saveAll(questions);
        log.info("Successfully saved {} questions", questions.size());
        return questions;
    }

    @Override
    public List<Question> getAllByTypeAndCategory(final QuestionType type, final String category) {
        log.info("Fetching questions by type '{}' and category '{}'", type, category);
        final var questions = repository.findByTypeAndCategories_NameContains(type, category);
        log.info("Found {} questions for type '{}' and category '{}'", questions.size(), type, category);
        return questions;
    }

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException if no question with the given ID was found
     */
    @Override
    @Transactional(readOnly = true)
    public QuestionDto getById(final long id, final boolean revealCorrect) {
        log.info("Fetching question with id '{}'", id);
        return repository.findById(id)
                .map(question -> mapper.toDto(question, revealCorrect))
                .orElseThrow(() -> {
                    final var message = "Question with id '" + id + "' not found.";
                    log.warn(message);
                    return new EntityNotFoundException(message);
                });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Counts the matching questions and fetches the one at a random offset, ordered by ID.
     *
     * @throws EntityNotFoundException if no question matches the category and type
     */
    @Override
    @Transactional(readOnly = true)
    public QuestionDto getRandom(@Nullable final CategoryVariant category, @Nullable final QuestionType type) {
        log.info("Fetching random question by category '{}' and type '{}'", category, type);
        final var categoryName = Objects.toString(category, null);
        final var count = repository.countMatching(type, categoryName);
        if (count == 0) {
            throw randomQuestionNotFound(category, type);
        }
        final var page = PageRequest.of(ThreadLocalRandom.current().nextInt(Math.toIntExact(count)), 1);
        return repository.findMatching(type, categoryName, page).stream()
                .findFirst()
                .map(question -> mapper.toDto(question, true))
                // a question may be deleted between the count and the fetch
                .orElseThrow(() -> randomQuestionNotFound(category, type));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionDto> getAll(final Pageable pageable) {
        return repository.findAll(pageable)
                .map(question -> mapper.toDto(question, true));
    }

    private static EntityNotFoundException randomQuestionNotFound(@Nullable final CategoryVariant category,
                                                                  @Nullable final QuestionType type) {
        final var message = "No question found for category '" + Objects.toString(category, "any")
                + "' and type '" + Objects.toString(type, "any") + "'.";
        log.warn(message);
        return new EntityNotFoundException(message);
    }

}
