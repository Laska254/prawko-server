package pl.prawko.prawko_server.service.implementation;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.dto.QuestionDto;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.repository.CategoryRepository;
import pl.prawko.prawko_server.repository.QuestionRepository;
import pl.prawko.prawko_server.service.ICategoryService;
import pl.prawko.prawko_server.service.IQuestionService;
import pl.prawko.prawko_server.util.CSVParser;

import java.util.List;

/**
 * Implementation of {@link ICategoryService} that managing {@link Category} entities using a {@link CategoryRepository} and mapping CSV file to
 * {@link Question} entities.
 */
@Service
public class QuestionService implements IQuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    @NonNull
    private final QuestionRepository repository;
    @NonNull
    private final QuestionMapper mapper;
    @NonNull
    private final CSVParser parser;

    /**
     * Constructs a new {@code QuestionService} with the given repository and mapper.
     *
     * @param repository the {@link QuestionRepository} used to persist {@link Question} entities
     * @param parser     the {@link CSVParser} used to parse file to {@link Question} entities
     */
    public QuestionService(@NonNull final QuestionRepository repository,
                           @NonNull final QuestionMapper mapper,
                           @NonNull final CSVParser parser) {
        this.repository = repository;
        this.mapper = mapper;
        this.parser = parser;
    }

    /**
     * {@inheritDoc}
     *
     * @throws MultipartException if the file is not of type "text/csv"
     * @throws RuntimeException   if there is an error reading or parsing CSV file
     */
    @Override
    public List<Question> parseFileToQuestions(@NonNull final MultipartFile file) {
        log.info("Attempting to parse file '{}'", file.getOriginalFilename());
        return parser.parseFileToQuestions(file);
    }

    @Override
    public void saveAll(@NonNull final List<Question> questions) {
        log.info("Saving {} question(s)", questions.size());
        repository.saveAll(questions);
        log.info("Successfully saved {} questions", questions.size());
    }

    @Override
    public List<Question> getAllByTypeAndCategory(@NonNull final QuestionType type, @NonNull final String category) {
        log.info("Fetching questions by type '{}' and category '{}'", type, category);
        final var questions = repository.findByTypeAndCategories_NameContains(type, category);
        log.info("Found {} questions for type '{}' and category '{}'", questions.size(), type, category);
        return questions;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionDto getById(long id) {
        log.info("Fetching question with id '{}'", id);
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> {
                    final var message = "Question with id '" + id + "' not found.";
                    log.warn(message);
                    return new EntityNotFoundException(message);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDto> getAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

}
