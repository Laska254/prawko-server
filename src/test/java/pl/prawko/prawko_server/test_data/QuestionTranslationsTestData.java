package pl.prawko.prawko_server.test_data;

import pl.prawko.prawko_server.dto.QuestionTranslationDto;
import pl.prawko.prawko_server.model.Language;
import pl.prawko.prawko_server.model.QuestionTranslation;
import pl.prawko.prawko_server.model.QuestionType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static pl.prawko.prawko_server.test_data.LanguageTestData.DE;
import static pl.prawko.prawko_server.test_data.LanguageTestData.EN;
import static pl.prawko.prawko_server.test_data.LanguageTestData.PL;

public class QuestionTranslationsTestData {

    private static final Map<QuestionType, Map<Language, String>> TRANSLATIONS = Map.ofEntries(
            Map.entry(QuestionType.BASIC, Map.ofEntries(
                    Map.entry(PL, "Czy w przedstawionej sytuacji masz prawo - mimo podawanego sygnału - skręcić w prawo?"),
                    Map.entry(EN, "Are you allowed in this situation to turn right despite the light displayed?"),
                    Map.entry(DE, "Darfst du in der dargestellten Situation - trotz des gegebenen Signals - rechts abbiegen?")
            )),
            Map.entry(QuestionType.SPECIAL, Map.ofEntries(
                    Map.entry(PL, "Jak często należy obracać poszkodowanego nieurazowego na drugi bok po ułożeniu go w pozycji bezpiecznej?"),
                    Map.entry(EN, "How often should you turn a non-traumatic victim to the other side after laying him in the recovery position?"),
                    Map.entry(DE, "Wie oft soll man einen symptomlosen Betroffenen auf die andere Körperseite nach dem Legen in stabiler Seitenlage drehen?"))));

    private QuestionTranslationsTestData() {
    }

    static List<QuestionTranslation> create(final QuestionType type) {
        return TRANSLATIONS.get(type).entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().getId()))
                .map(e -> new QuestionTranslation()
                        .setContent(e.getValue())
                        .setLanguage(e.getKey()))
                .toList();
    }

    static List<QuestionTranslationDto> createDtos(final QuestionType type) {
        return TRANSLATIONS.get(type).entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().getId()))
                .map(e -> new QuestionTranslationDto(e.getValue(), e.getKey().getCode()))
                .toList();
    }

}
