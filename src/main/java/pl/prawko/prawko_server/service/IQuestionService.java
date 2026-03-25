package pl.prawko.prawko_server.service;

import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.dto.QuestionDto;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCSV;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.util.CSVParser;

import java.util.List;

/**
 * Service interface for managing {@link Question} entities.
 * <p>
 * Provides operations for parsing questions from CSV files, persisting questions to the database,
 * and retrieving questions by type and category.
 * </p>
 */
public interface IQuestionService {

    /**
     * Parses the provided CSV file into a list of {@link Question} entities
     * using {@link CSVParser} and imports them to the database.
     * <p>
     * The CSV file is expected to have a header row and use commas as column separators.
     * Each row is mapped to a {@link QuestionCSV} object, which is converted to a {@link Question} entity using {@link QuestionMapper}.
     * </p>
     *
     * @param file the CSV file containing questions data
     * @return a list of {@link Question} entities
     */
    List<Question> parseFileToQuestions(MultipartFile file);

    /**
     * Retrieves all questions by {@link QuestionType} and {@link Category}'s name.
     *
     * @param type     the type of questions to retrieve
     * @param category the name of the category to filter by
     * @return a list of all matching questions
     */
    List<Question> getAllByTypeAndCategory(QuestionType type, String category);

    /**
     * Retrieves a question by its ID.
     *
     * @param id the ID of the question to retrieve
     * @return the question as a {@link QuestionDto}
     */
    QuestionDto getById(long id);

    /**
     * Returns a list of all questions converted to DTO.
     *
     * @return a list of {@link QuestionDto}
     */
    List<QuestionDto> getAll();

}
