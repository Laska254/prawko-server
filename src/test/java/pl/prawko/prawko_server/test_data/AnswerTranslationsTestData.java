package pl.prawko.prawko_server.test_data;

import pl.prawko.prawko_server.dto.AnswerTranslationDto;
import pl.prawko.prawko_server.model.AnswerTranslation;

import java.util.List;

import static pl.prawko.prawko_server.test_data.LanguageTestData.DE;
import static pl.prawko.prawko_server.test_data.LanguageTestData.EN;
import static pl.prawko.prawko_server.test_data.LanguageTestData.PL;

public class AnswerTranslationsTestData {

    private AnswerTranslationsTestData() {
    }

    static List<AnswerTranslation> variantA() {
        return List.of(
                new AnswerTranslation().setLanguage(PL).setContent("Co 60 minut."),
                new AnswerTranslation().setLanguage(EN).setContent("Every 60 minutes."),
                new AnswerTranslation().setLanguage(DE).setContent("jede 60 Minuten")
        );
    }

    static List<AnswerTranslation> variantB() {
        return List.of(
                new AnswerTranslation().setLanguage(PL).setContent("Co 30 minut."),
                new AnswerTranslation().setLanguage(EN).setContent("Every 30 minutes."),
                new AnswerTranslation().setLanguage(DE).setContent("jede 30 Minuten")
        );
    }

    static List<AnswerTranslation> variantC() {
        return List.of(
                new AnswerTranslation().setLanguage(PL).setContent("Co 15 minut."),
                new AnswerTranslation().setLanguage(EN).setContent("Every 15 minutes."),
                new AnswerTranslation().setLanguage(DE).setContent("jede 15 Minuten")
        );
    }

    static List<AnswerTranslationDto> createAnswerTranslationsDtos(final AnswerVariant variant) {
        return createAnswerTranslations(variant).stream()
                .map(translation -> new AnswerTranslationDto(
                        translation.getContent(),
                        translation.getLanguage().getCode()))
                .toList();
    }

    static List<AnswerTranslation> createAnswerTranslations(final AnswerVariant variant) {
        return switch (variant) {
            case A -> AnswerTranslationsTestData.variantA();
            case B -> AnswerTranslationsTestData.variantB();
            case C -> AnswerTranslationsTestData.variantC();
            default -> throw new IllegalStateException("Unexpected value: " + variant);
        };
    }

}
