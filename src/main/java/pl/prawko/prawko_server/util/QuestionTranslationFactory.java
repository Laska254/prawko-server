package pl.prawko.prawko_server.util;

import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.model.Language;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCsv;
import pl.prawko.prawko_server.model.QuestionTranslation;
import pl.prawko.prawko_server.service.ILanguageService;

import java.util.Comparator;
import java.util.List;

@Component
public class QuestionTranslationFactory {

    private final ILanguageService languageService;

    public QuestionTranslationFactory(final ILanguageService languageService) {
        this.languageService = languageService;
    }

    public List<QuestionTranslation> createTranslations(final QuestionCsv questionCsv, final Question question) {
        return languageService.findAll().stream()
                .sorted(Comparator.comparing(Language::getId))
                .map(language -> createTranslation(questionCsv, question, language))
                .toList();
    }

    private QuestionTranslation createTranslation(final QuestionCsv questionCsv,
                                                  final Question question,
                                                  final Language language) {
        return new QuestionTranslation()
                .setQuestion(question)
                .setLanguage(language)
                .setContent(resolveContent(questionCsv, language));
    }

    private String resolveContent(final QuestionCsv questionCsv, final Language language) {
        return switch (language.getCode()) {
            case Language.PL -> questionCsv.contentPL();
            case Language.EN -> questionCsv.contentEN();
            case Language.DE -> questionCsv.contentDE();
            default -> throw new IllegalStateException("Unexpected language: " + language.getCode());
        };
    }

}
