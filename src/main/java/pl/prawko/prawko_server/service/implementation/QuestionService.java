package pl.prawko.prawko_server.service.implementation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCSV;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.repository.CategoryRepository;
import pl.prawko.prawko_server.repository.QuestionRepository;
import pl.prawko.prawko_server.service.ICategoryService;
import pl.prawko.prawko_server.service.IQuestionService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    /**
     * Constructs a new {@code QuestionService} with the given repository and mapper.
     *
     * @param repository the {@link QuestionRepository} used to persist {@link Question} entities
     * @param mapper     the {@link QuestionMapper} used to map {@link QuestionCSV} to {@link Question} entity
     */
    public QuestionService(@NonNull final QuestionRepository repository,
                           @NonNull final QuestionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     *
     * @throws MultipartException if the file is not of type "text/csv"
     * @throws RuntimeException   if there is an error reading or parsing CSV file
     */
    @Override
    public List<Question> parseFileToQuestions(@NonNull final MultipartFile file) {
        log.debug("Attempting to parse file '{}'", file.getOriginalFilename());
        if (!"text/csv".equals(file.getContentType())) {
            final var message = "Invalid file format.";
            log.warn("{} '{}'", message, file.getContentType());
            throw new MultipartException(message);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            final var csvMapper = new CsvMapper();
            csvMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            final var schema = CsvSchema.emptySchema()
                    .withHeader()
                    .withColumnSeparator(',')
                    .withQuoteChar('"');
            final MappingIterator<QuestionCSV> csvRows = csvMapper
                    .readerFor(QuestionCSV.class)
                    .with(schema)
                    .readValues(reader);
            final var questionCSVs = csvRows.readAll();
            log.debug("Parsed {} rows from file '{}'", questionCSVs.size(), file.getOriginalFilename());
            final var questions = mapQuestionCSVModelsToQuestions(questionCSVs);
            log.info("Successfully mapped {} questions from file '{}'", questions.size(), file.getOriginalFilename());
            return questions;
        } catch (IOException exception) {
            final var message = "CSV file failed to parse: ";
            log.error("{} '{}': {}", message, file.getOriginalFilename(), exception.getMessage(), exception);
            throw new RuntimeException(message + exception.getMessage());
        }
    }

    @Override
    public void saveAll(@NonNull final List<Question> questions) {
        log.debug("Saving {} question(s)", questions.size());
        repository.saveAll(questions);
        log.info("Successfully saved {} questions", questions.size());
    }

    /**
     * Maps a list of {@link QuestionCSV} models to a list of {@link Question} using {@link QuestionMapper}
     *
     * @param questionCSVs the list of CSV models to map
     * @return the list of mapped {@link Question} entities
     */
    private List<Question> mapQuestionCSVModelsToQuestions(@NonNull final List<QuestionCSV> questionCSVs) {
        log.debug("Mapping {} questions", questionCSVs.size());
        return questionCSVs.stream()
                .map(mapper::mapQuestionCSVToQuestion)
                .toList();
    }

    @Override
    public List<Question> getAllByTypeAndCategory(@NonNull final QuestionType type, @NonNull final String category) {
        log.debug("Fetching questions by type '{}' and category '{}'", type, category);
        final var questions = repository.findByTypeAndCategories_NameContains(type, category);
        log.debug("Found {} questions for type '{}' and category '{}'", questions.size(), type, category);
        return questions;
    }

}
