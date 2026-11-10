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

@Component
public class AnswerFactory {

    private final ILanguageService languageService;

    public AnswerFactory(final ILanguageService languageService) {
        this.languageService = languageService;
    }

    public List<Answer> create(final QuestionCsv questionCsv, final Question question) {
        return switch (QuestionType.ofType(questionCsv.type())) {
            case BASIC -> basic(question);
            case SPECIAL -> special(questionCsv, question);
        };
    }

    private List<Answer> basic(final Question question) {
        return List.of(
                new Answer().setQuestion(question).setCorrect(true),
                new Answer().setQuestion(question).setCorrect(false)
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
