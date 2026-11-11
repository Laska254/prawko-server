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
import java.util.stream.Stream;

/**
 * Creates the answers of an imported question.
 * <p>
 * A {@link QuestionType#BASIC} question gets two answers without translations in a fixed order, yes first, then no;
 * the one named by {@code Poprawna odp} ({@code T} or {@code N}) is correct. A {@link QuestionType#SPECIAL} question
 * gets answers {@code A}, {@code B} and {@code C} in every language.
 */
@Component
public class AnswerFactory {

    private static final char YES = 'T';
    private static final char NO = 'N';

    private final ILanguageService languageService;

    public AnswerFactory(final ILanguageService languageService) {
        this.languageService = languageService;
    }

    public List<Answer> create(final QuestionCsv questionCsv, final Question question) {
        return switch (question.getType()) {
            case BASIC -> basic(questionCsv, question);
            case SPECIAL -> special(questionCsv, question);
        };
    }

    private List<Answer> basic(final QuestionCsv questionCsv, final Question question) {
        return List.of(
                new Answer().setQuestion(question).setCorrect(questionCsv.correctAnswer() == YES),
                new Answer().setQuestion(question).setCorrect(questionCsv.correctAnswer() == NO)
        );
    }

    private List<Answer> special(final QuestionCsv questionCsv, final Question question) {
        final var languages = languageService.findAll();
        return Stream.of('A', 'B', 'C')
                .map(label -> build(questionCsv, question, languages, label))
                .toList();
    }

    private Answer build(final QuestionCsv questionCsv,
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
