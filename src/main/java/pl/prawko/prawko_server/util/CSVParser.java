package pl.prawko.prawko_server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.mapper.AnswerMapper;
import pl.prawko.prawko_server.model.Language;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCSV;
import pl.prawko.prawko_server.model.QuestionTranslation;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.implementation.CategoryService;
import pl.prawko.prawko_server.service.implementation.LanguageService;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

@Component
public class CSVParser {

    private static final Logger log = LoggerFactory.getLogger(CSVParser.class);

    private final CsvMapper csvMapper;
    private final CsvSchema csvSchema;
    private final AnswerMapper answerMapper;
    private final LanguageService languageService;
    private final CategoryService categoryService;

    public CSVParser(final AnswerMapper answerMapper,
                     final LanguageService languageService,
                     final CategoryService categoryService) {
        this.answerMapper = answerMapper;
        this.languageService = languageService;
        this.categoryService = categoryService;
        this.csvMapper = CsvMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        this.csvSchema = CsvSchema.emptySchema()
                .withHeader()
                .withColumnSeparator(',')
                .withQuoteChar('"');
    }

    public List<Question> parseFileToQuestions(final MultipartFile file) {
        validate(file);
        try (var reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            final MappingIterator<QuestionCSV> csvRows = csvMapper
                    .readerFor(QuestionCSV.class)
                    .with(csvSchema)
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

    private Question mapQuestionCSVToQuestion(final QuestionCSV questionCSV) {
        final var question = new Question()
                .setId(questionCSV.id())
                .setName(questionCSV.name())
                .setType(QuestionType.ofType(questionCSV.type()))
                .setMedia(questionCSV.mediaName().replaceAll("\\.wmv$", ".webm"))
                .setPoints(questionCSV.value())
                .setCategories(categoryService.findAllFromString(questionCSV.categories()));
        return question
                .setTranslations(mapQuestionTranslations(questionCSV, question))
                .setAnswers(answerMapper.fromQuestionCSVToAnswers(questionCSV, question));
    }

    private void validate(final MultipartFile file) {
        log.debug("Validating file '{}'", file.getOriginalFilename());
        if (!"text/csv".equals(file.getContentType())) {
            final var message = "Invalid file format.";
            log.warn("{} '{}'", message, file.getContentType());
            throw new MultipartException(message);
        }
    }

    private List<Question> mapQuestionCSVModelsToQuestions(final List<QuestionCSV> questionCSVs) {
        log.debug("Mapping {} questions", questionCSVs.size());
        return questionCSVs.stream()
                .map(this::mapQuestionCSVToQuestion)
                .toList();
    }

    private List<QuestionTranslation> mapQuestionTranslations(final QuestionCSV questionCSV,
                                                              final Question question) {
        return languageService.findAll().stream()
                .sorted(Comparator.comparing(Language::getId))
                .map(language -> new QuestionTranslation()
                        .setQuestion(question)
                        .setLanguage(language)
                        .setContent(getContent(questionCSV, language)))
                .toList();
    }

    private String getContent(final QuestionCSV questionCSV,
                              final Language language) {
        return switch (language.getCode()) {
            case Language.PL -> questionCSV.contentPL();
            case Language.EN -> questionCSV.contentEN();
            case Language.DE -> questionCSV.contentDE();
            default -> throw new IllegalStateException("Unexpected language: " + language.getCode());
        };
    }

}

