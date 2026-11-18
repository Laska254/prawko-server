package pl.prawko.prawko_server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.exception.UnknownCorrectAnswerException;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCsv;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.ICategoryService;

/**
 * Maps a CSV row to a {@link Question} with its translations, answers and categories.
 * <p>
 * The type follows the correct answer ({@link QuestionType#ofCorrectAnswer(char)}), since the answers are built from
 * it. {@code Zakres struktury} only gets a warning when it names another type (ignoring case) or none: the catalogue
 * has such rows, and rejecting them would mean editing it by hand after every update.
 */
@Component
public class CsvFacade {

    private static final Logger log = LoggerFactory.getLogger(CsvFacade.class);

    private final ICategoryService categoryService;
    private final QuestionMapper questionMapper;
    private final QuestionTranslationFactory translationFactory;
    private final AnswerFactory answerFactory;

    public CsvFacade(final QuestionMapper questionMapper,
                     final QuestionTranslationFactory translationFactory,
                     final AnswerFactory answerFactory,
                     final ICategoryService categoryService) {
        this.questionMapper = questionMapper;
        this.translationFactory = translationFactory;
        this.answerFactory = answerFactory;
        this.categoryService = categoryService;
    }

    /**
     * Maps a CSV row to a question.
     *
     * @param questionCsv the row
     * @return the question with its associations
     * @throws UnknownCorrectAnswerException if the correct answer isn't {@code T}, {@code N}, {@code A}, {@code B} or
     *                                       {@code C}
     */
    public Question mapSingleRow(final QuestionCsv questionCsv) {
        final var type = resolveType(questionCsv);
        final var question = questionMapper.toEntity(questionCsv)
                .setType(type);
        question.setTranslations(translationFactory.createTranslations(questionCsv, question));
        question.setAnswers(answerFactory.create(questionCsv, question));
        question.setCategories(categoryService.findAllFromString(questionCsv.categories()));
        return question;
    }

    private static QuestionType resolveType(final QuestionCsv questionCsv) {
        final var type = QuestionType.ofCorrectAnswer(questionCsv.correctAnswer())
                .orElseThrow(() -> new UnknownCorrectAnswerException(questionCsv.correctAnswer()));
        if (!type.getName().equalsIgnoreCase(questionCsv.type())) {
            // not the raw value: the cell may hold anything
            log.warn("Question {}: 'Zakres struktury' doesn't name the type of correct answer '{}', importing it as {}",
                    questionCsv.id(), questionCsv.correctAnswer(), type);
        }
        return type;
    }

}
