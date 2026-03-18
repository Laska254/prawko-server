package pl.prawko.prawko_server.service.implementation;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.dto.QuestionDto;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.repository.QuestionRepository;
import pl.prawko.prawko_server.service.IQuestionService;
import pl.prawko.prawko_server.util.CSVParser;

import java.util.List;

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
        return parser.parseFileToQuestions(file);
    }

    @Override
    public void saveAll(final List<Question> questions) {
        log.info("Saving {} question(s)", questions.size());
        repository.saveAll(questions);
        log.info("Successfully saved {} questions", questions.size());
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
     * @throws EntityNotFoundException if no question with the given ID is found
     */
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
