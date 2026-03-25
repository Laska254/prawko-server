package pl.prawko.prawko_server.util;

import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.AnswerTranslation;
import pl.prawko.prawko_server.model.Language;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCSV;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.implementation.LanguageService;

import java.util.List;
import java.util.stream.Stream;

@Component
public class AnswerFactory {

    private final LanguageService languageService;

    public AnswerFactory(final LanguageService languageService) {
        this.languageService = languageService;
    }

    public List<Answer> create(final QuestionCSV questionCSV, final Question question) {
        return switch (QuestionType.ofType(questionCSV.type())) {
            case BASIC -> basic(question);
            case SPECIAL -> special(questionCSV, question);
        };
    }

    private List<Answer> basic(final Question question) {
        return List.of(
                new Answer().setQuestion(question).setCorrect(true),
                new Answer().setQuestion(question).setCorrect(false)
        );
    }

    private List<Answer> special(final QuestionCSV questionCSV, final Question question) {
        var languages = languageService.findAll();
        return Stream.of('A', 'B', 'C')
                .map(label -> build(questionCSV, question, languages, label))
                .toList();
    }

    private Answer build(final QuestionCSV questionCSV,
                         final Question question,
                         final List<Language> languages,
                         final char label) {
        final var answer = new Answer()
                .setQuestion(question)
                .setCorrect(label == questionCSV.correctAnswer());
        final var translations = languages.stream()
                .map(language -> new AnswerTranslation()
                        .setAnswer(answer)
                        .setLanguage(language)
                        .setContent(
                                questionCSV.getAnswersTranslations()
                                        .get(language.getCode())
                                        .get(label)
                        ))
                .toList();
        return answer.setTranslations(translations);
    }

}
