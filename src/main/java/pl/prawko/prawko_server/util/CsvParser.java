package pl.prawko.prawko_server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.exception.InvalidCsvException;
import pl.prawko.prawko_server.exception.UnknownCategoryException;
import pl.prawko.prawko_server.model.CategoryVariant;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCsv;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CsvParser {

    private static final Logger log = LoggerFactory.getLogger(CsvParser.class);
    private static final String ALLOWED_CATEGORIES = Arrays.stream(CategoryVariant.values())
            .map(CategoryVariant::name)
            .collect(Collectors.joining(", "));

    private final CsvMapper csvMapper;
    private final CsvSchema csvSchema;
    private final CsvFacade csvFacade;

    public CsvParser(final CsvFacade csvFacade) {
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
        try (final var reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            final MappingIterator<QuestionCsv> csvRows = csvMapper
                    .readerFor(QuestionCsv.class)
                    .with(csvSchema)
                    .readValues(reader);
            final var questions = new ArrayList<Question>();
            final var problems = new ArrayList<String>();
            while (csvRows.hasNextValue()) {
                // after hasNextValue the location is the record's first line, also for multi-line quoted values
                final var line = csvRows.currentLocation().getLineNr();
                try {
                    questions.add(csvFacade.mapSingleRow(csvRows.nextValue()));
                } catch (final UnknownCategoryException exception) {
                    exception.getNames()
                            .forEach(name -> problems.add("line " + line + ": unknown category '" + name + "'"));
                }
            }
            if (!problems.isEmpty()) {
                // only the count: the list is unbounded and echoes raw cell values
                log.warn("Invalid CSV file: {} unknown categories. File: '{}'",
                        problems.size(), file.getOriginalFilename());
                throw new InvalidCsvException("Invalid CSV file: " + String.join("; ", problems)
                        + ". Allowed: " + ALLOWED_CATEGORIES + ".");
            }
            log.info("Successfully mapped {} questions from file '{}'", questions.size(), file.getOriginalFilename());
            return questions;
        } catch (final JacksonException exception) {
            final var message = describe(exception);
            log.warn("{} File: '{}'", message, file.getOriginalFilename());
            throw new InvalidCsvException(message);
        } catch (final IOException exception) {
            final var message = "CSV file failed to parse: ";
            log.error("{} '{}': {}", message, file.getOriginalFilename(), exception.getMessage(), exception);
            throw new UncheckedIOException(message + exception.getMessage(), exception);
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

    private static String describe(final JacksonException exception) {
        final var location = exception.getLocation();
        final String line;
        if (location == null) {
            line = "";
        } else {
            line = " at line " + location.getLineNr();
        }
        final var path = exception.getPath();
        final String column;
        if (path.isEmpty()) {
            column = "";
        } else {
            column = " in column '" + path.getLast().getPropertyName() + "'";
        }
        return "Invalid CSV file" + line + column + ": " + exception.getOriginalMessage();
    }

}

