package pl.prawko.prawko_server.util;

import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.AnswerTranslation;
import pl.prawko.prawko_server.model.Language;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCsv;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.ILanguageService;

import java.util.List;

/**
 * Creates the answers of an imported question.
 * <p>
 * A question gets one answer per label of its type ({@link QuestionType#getAnswerLabels()}), in that order; the one
 * named by {@code Poprawna odp} is correct. Only {@link QuestionType#SPECIAL} answers have translations, in every
 * language; {@link QuestionType#BASIC} ones (yes, no) have none.
 */
@Component
public class AnswerFactory {

    private final ILanguageService languageService;

    public AnswerFactory(final ILanguageService languageService) {
        this.languageService = languageService;
    }

    public List<Answer> create(final QuestionCsv questionCsv, final Question question) {
        final var type = question.getType();
        final var languages = switch (type) {
            case BASIC -> List.<Language>of();
            case SPECIAL -> languageService.findAll();
        };
        return type.getAnswerLabels()
                .chars()
                .mapToObj(label -> build(questionCsv, question, languages, (char) label))
                .toList();
    }

    private static Answer build(final QuestionCsv questionCsv,
                                final Question question,
                                final List<Language> languages,
                                final char label) {
        final var answer = new Answer()
                .setQuestion(question)
                .setCorrect(label == questionCsv.correctAnswer());
        final var translations = languages.stream()
                .map(language -> new AnswerTranslation()
                        .setAnswer(answer)
                        .setLanguage(language)
                        .setContent(
                                questionCsv.getAnswersTranslations()
                                        .get(language.getCode())
                                        .get(label)
                        ))
                .toList();
        return answer.setTranslations(translations);
    }

}
