package pl.prawko.prawko_server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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

import java.util.Comparator;
import java.util.List;

@Component
public class QuestionCsvMapper {

    private static final Logger log = LoggerFactory.getLogger(QuestionCsvMapper.class);

    private static final List<Character> SPECIAL_LABELS = List.of('A', 'B', 'C');

    private final QuestionMapper questionMapper;

    private final LanguageService languageService;

    private final CategoryService categoryService;

    public QuestionCsvMapper(final QuestionMapper questionMapper,
                             final LanguageService languageService,
                             final CategoryService categoryService) {
        this.questionMapper = questionMapper;
        this.languageService = languageService;
        this.categoryService = categoryService;
    }

    public List<Question> CSVsToEntities(final List<QuestionCSV> questionCSVs) {
        log.debug("Mapping {} questions", questionCSVs.size());
        return questionCSVs.stream()
                .map(this::mapQuestionCSVToQuestion)
                .toList();
    }

    private Question mapQuestionCSVToQuestion(final QuestionCSV questionCSV) {
        var question = questionMapper.toEntity(questionCSV);
        return question
                .setTranslations(mapQuestionTranslations(questionCSV, question))
                .setAnswers(fromQuestionCSVToAnswers(questionCSV, question))
                .setCategories(categoryService.findAllFromString(questionCSV.categories()));
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
