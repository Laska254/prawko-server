package pl.prawko.prawko_server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCSV;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class CSVParser {

    private static final Logger log = LoggerFactory.getLogger(CSVParser.class);

    private final CsvMapper csvMapper;
    private final CsvSchema csvSchema;
    private final CsvFacade csvFacade;

    public CSVParser(final CsvFacade csvFacade) {
        this.csvFacade = csvFacade;
        this.csvMapper = CsvMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        this.csvSchema = CsvSchema.emptySchema()
                .withHeader()
                .withColumnSeparator(',')
                .withQuoteChar('"');
    }

    public List<Question> parse(final MultipartFile file) {
        validate(file);
        try (var reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            final MappingIterator<QuestionCSV> csvRows = csvMapper
                    .readerFor(QuestionCSV.class)
                    .with(csvSchema)
                    .readValues(reader);
            final var questionCSVs = csvRows.readAll();
            log.info("Parsed {} rows from file '{}'", questionCSVs.size(), file.getOriginalFilename());
            final var questions = csvFacade.mapCsvRows(questionCSVs);
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

}

