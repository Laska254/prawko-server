package pl.prawko.prawko_server.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCSV;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class CSVParser {

    private static final Logger log = LoggerFactory.getLogger(CSVParser.class);

    private final QuestionMapper mapper;

    public CSVParser(final QuestionMapper mapper) {
        this.mapper = mapper;
    }

    public List<Question> parseFileToQuestions(final MultipartFile file) {
        validate(file);
        try (var reader = new BufferedReader(
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
            log.info("Parsed {} rows from file '{}'", questionCSVs.size(), file.getOriginalFilename());
            final var questions = mapQuestionCSVModelsToQuestions(questionCSVs);
            log.info("Successfully mapped {} questions from file '{}'", questions.size(), file.getOriginalFilename());
            return questions;
        } catch (IOException exception) {
            final var message = "CSV file failed to parse: ";
            log.error("{} '{}': {}", message, file.getOriginalFilename(), exception.getMessage(), exception);
            throw new RuntimeException(message + exception.getMessage());
        }
    }

    private void validate(final MultipartFile file) {
        log.debug("Validating file '{}'", file.getOriginalFilename());
        if (!"text/csv".equals(file.getContentType())) {
            final var message = "Invalid file format.";
            log.warn("{} '{}'", message, file.getContentType());
            throw new MultipartException(message);
        }
    }

    /**
     * Maps a list of {@link QuestionCSV} models to a list of {@link Question} using {@link QuestionMapper}
     *
     * @param questionCSVs the list of CSV models to map
     * @return the list of mapped {@link Question} entities
     */
    private List<Question> mapQuestionCSVModelsToQuestions(final List<QuestionCSV> questionCSVs) {
        log.debug("Mapping {} questions", questionCSVs.size());
        return questionCSVs.stream()
                .map(mapper::mapQuestionCSVToQuestion)
                .toList();
    }

}

