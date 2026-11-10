package pl.prawko.prawko_server.util;

import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.model.Language;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCSV;
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

    public List<QuestionTranslation> createTranslations(final QuestionCSV questionCSV, final Question question) {
        return languageService.findAll().stream()
                .sorted(Comparator.comparing(Language::getId))
                .map(language -> createTranslation(questionCSV, question, language))
                .toList();
    }

    private QuestionTranslation createTranslation(final QuestionCSV questionCSV,
                                                  final Question question,
                                                  final Language language) {
        return new QuestionTranslation()
                .setQuestion(question)
                .setLanguage(language)
                .setContent(resolveContent(questionCSV, language));
    }

    private String resolveContent(final QuestionCSV questionCSV, final Language language) {
        return switch (language.getCode()) {
            case Language.PL -> questionCSV.contentPL();
            case Language.EN -> questionCSV.contentEN();
            case Language.DE -> questionCSV.contentDE();
            default -> throw new IllegalStateException("Unexpected language: " + language.getCode());
        };
    }

}
