package pl.prawko.prawko_server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.AnswerTranslation;
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
    private static final List<Character> SPECIAL_LABELS = List.of('A', 'B', 'C');

    private final CsvMapper csvMapper;
    private final CsvSchema csvSchema;
    private final LanguageService languageService;
    private final CategoryService categoryService;
    private final QuestionMapper questionMapper;

    public CSVParser(final LanguageService languageService,
                     final CategoryService categoryService,
                     final QuestionMapper questionMapper) {
        this.languageService = languageService;
        this.categoryService = categoryService;
        this.questionMapper = questionMapper;
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
            final var questions = mapQuestionCSVModelsToQuestions(questionCSVs);
            log.info("Successfully mapped {} questions from file '{}'", questions.size(), file.getOriginalFilename());
            return questions;
        } catch (IOException exception) {
            final var message = "CSV file failed to parse: ";
            log.error("{} '{}': {}", message, file.getOriginalFilename(), exception.getMessage(), exception);
            throw new RuntimeException(message + exception.getMessage());
        }
    }

    /**
     * This method recognizes question types and creates basic or special answers.
     *
     * @param questionCSV CSV model to map answers from
     * @param question    {@link Question} entity that answers would be linked to
     * @return list of basic or special {@link Answer} entities
     */
    public List<Answer> fromQuestionCSVToAnswers(final QuestionCSV questionCSV,
                                                 final Question question) {
        return switch (QuestionType.ofType(questionCSV.type())) {
            case BASIC -> List.of(
                    new Answer()
                            .setQuestion(question)
                            .setCorrect(true),
                    new Answer()
                            .setQuestion(question)
                            .setCorrect(false)
            );
            case SPECIAL -> mapSpecialQuestionAnswers(questionCSV, question);
        };
    }

    /**
     * This method is responsible for creating special {@link Answer} with their translations from the CSV model.
     *
     * @param questionCSV CSV model to map answers with translations from
     * @param question    {@link Question} entity that answers would be linked to
     * @return list of special {@link Answer} entities
     */
    private List<Answer> mapSpecialQuestionAnswers(final QuestionCSV questionCSV,
                                                   final Question question) {
        final var languages = languageService.findAll();
        return SPECIAL_LABELS.stream()
                .map(label -> {
                    final var answer = new Answer()
                            .setQuestion(question)
                            .setCorrect(label == questionCSV.correctAnswer());
                    final var translations = languages.stream()
                            .map(language -> new AnswerTranslation()
                                    .setLanguage(language)
                                    .setAnswer(answer)
                                    .setContent(
                                            questionCSV.getAnswersTranslations()
                                                    .get(language.getCode())
                                                    .get(label)))
                            .toList();
                    return answer.setTranslations(translations);
                })
                .toList();
    }

    private Question mapQuestionCSVToQuestion(final QuestionCSV questionCSV) {
        var question = questionMapper.toEntity(questionCSV);
        return question
                .setTranslations(mapQuestionTranslations(questionCSV, question))
                .setAnswers(fromQuestionCSVToAnswers(questionCSV, question))
                .setCategories(categoryService.findAllFromString(questionCSV.categories()));
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

