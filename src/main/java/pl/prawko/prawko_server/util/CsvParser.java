package pl.prawko.prawko_server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.exception.InvalidQuestionFileException;
import pl.prawko.prawko_server.exception.UnknownCategoryException;
import pl.prawko.prawko_server.exception.UnknownCorrectAnswerException;
import pl.prawko.prawko_server.model.CategoryVariant;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCsv;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parses an uploaded CSV file into questions, all or nothing. An XLSX file is converted to CSV first by
 * {@link XlsxConverter}, so both formats go through the same import and give the same errors.
 * <p>
 * Rows without {@code Numer pytania} are skipped. Errors name the row as a spreadsheet numbers it: the header is
 * row 1, and a value spanning several lines still counts as one row.
 */
@Component
public class CsvParser {

    private static final Logger log = LoggerFactory.getLogger(CsvParser.class);
    private static final String CSV_CONTENT_TYPE = "text/csv";
    private static final int HEADER_ROW = 1;
    private static final String ID_COLUMN = "Numer pytania";
    private static final String ALLOWED_CATEGORIES = Arrays.stream(CategoryVariant.values())
            .map(CategoryVariant::name)
            .collect(Collectors.joining(", "));

    private final CsvMapper csvMapper;
    private final CsvSchema csvSchema;
    private final CsvFacade csvFacade;
    private final XlsxConverter xlsxConverter;

    public CsvParser(final CsvFacade csvFacade, final XlsxConverter xlsxConverter) {
        this.csvFacade = csvFacade;
        this.xlsxConverter = xlsxConverter;
        this.csvMapper = CsvMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        // the columns of QuestionCsv, matched to the header by name: a missing one fails the file
        // (CsvReadFeature.FAIL_ON_MISSING_HEADER_COLUMNS), so a renamed column doesn't import as null
        this.csvSchema = csvMapper.schemaFor(QuestionCsv.class)
                .withHeader()
                .withColumnReordering(true)
                .withColumnSeparator(',')
                .withQuoteChar('"');
    }

    public List<Question> parse(final MultipartFile file) {
        // the row being read; errors while reading the header keep the header row
        int row = HEADER_ROW;
        try (final var reader = open(file)) {
            // read as trees first: a row without an ID has empty values that can't bind to QuestionCsv
            final MappingIterator<JsonNode> csvRows = csvMapper
                    .readerFor(JsonNode.class)
                    .with(csvSchema)
                    .readValues(reader);
            final var questions = new ArrayList<Question>();
            final var problems = new ArrayList<String>();
            boolean unknownCategory = false;
            int skipped = 0;
            while (csvRows.hasNextValue()) {
                row++;
                final var csvRow = csvRows.nextValue();
                if (csvRow.path(ID_COLUMN).asString("").isBlank()) {
                    skipped++;
                    continue;
                }
                final var questionCsv = csvMapper.treeToValue(csvRow, QuestionCsv.class);
                try {
                    questions.add(csvFacade.mapSingleRow(questionCsv));
                } catch (final UnknownCorrectAnswerException exception) {
                    problems.add("row " + row + ": unknown correct answer '" + exception.getLabel()
                            + "', expected T, N, A, B or C");
                } catch (final UnknownCategoryException exception) {
                    unknownCategory = true;
                    for (final var name : exception.getNames()) {
                        problems.add("row " + row + ": unknown category '" + name + "'");
                    }
                }
            }
            if (!problems.isEmpty()) {
                // only the count: the list is unbounded and echoes raw cell values
                log.warn("Invalid CSV file: {} problems. File: '{}'", problems.size(), file.getOriginalFilename());
                final var message = new StringBuilder("Invalid CSV file: ")
                        .append(String.join("; ", problems))
                        .append('.');
                if (unknownCategory) {
                    message.append(" Allowed: ")
                            .append(ALLOWED_CATEGORIES)
                            .append('.');
                }
                throw new InvalidQuestionFileException(message.toString());
            }
            log.info("Successfully mapped {} questions from file '{}', skipped {} rows without '{}'",
                    questions.size(), file.getOriginalFilename(), skipped, ID_COLUMN);
            return questions;
        } catch (final JacksonException exception) {
            final var message = describe(exception, row);
            log.warn("{} File: '{}'", message, file.getOriginalFilename());
            throw new InvalidQuestionFileException(message);
        } catch (final IOException exception) {
            final var message = "CSV file failed to parse: ";
            log.error("{} '{}': {}", message, file.getOriginalFilename(), exception.getMessage(), exception);
            throw new UncheckedIOException(message + exception.getMessage(), exception);
        }
    }

    private Reader open(final MultipartFile file) throws IOException {
        final var contentType = file.getContentType();
        if (CSV_CONTENT_TYPE.equals(contentType)) {
            return new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        }
        if (XlsxConverter.CONTENT_TYPE.equals(contentType)) {
            try (final var xlsx = file.getInputStream()) {
                return new StringReader(xlsxConverter.toCsv(xlsx));
            }
        }
        final var message = "Invalid file format.";
        log.warn("{} '{}'", message, contentType);
        throw new MultipartException(message);
    }

    private static String describe(final JacksonException exception, final int row) {
        final var path = exception.getPath();
        final String column;
        if (path.isEmpty()) {
            column = "";
        } else {
            column = " in column '" + path.getLast().getPropertyName() + "'";
        }
        return "Invalid CSV file at row " + row + column + ": " + exception.getOriginalMessage();
    }

}

