package pl.prawko.prawko_server.test_data;

import pl.prawko.prawko_server.model.QuestionTranslation;
import pl.prawko.prawko_server.model.QuestionType;

import java.util.List;

import static pl.prawko.prawko_server.test_data.LanguageTestData.DE;
import static pl.prawko.prawko_server.test_data.LanguageTestData.EN;
import static pl.prawko.prawko_server.test_data.LanguageTestData.PL;

public class QuestionTranslationsTestData {

    private QuestionTranslationsTestData() {
    }

    static List<QuestionTranslation> create(final QuestionType type) {
        return switch (type) {
            case BASIC -> List.of(
                    new QuestionTranslation()
                            .setContent("Czy w przedstawionej sytuacji masz prawo - mimo podawanego sygnału - skręcić w prawo?")
                            .setLanguage(PL),
                    new QuestionTranslation()
                            .setContent("Are you allowed in this situation to turn right despite the light displayed?")
                            .setLanguage(EN),
                    new QuestionTranslation()
                            .setContent("Darfst du in der dargestellten Situation - trotz des gegebenen Signals - rechts abbiegen?")
                            .setLanguage(DE)
            );
            case SPECIAL -> List.of(
                    new QuestionTranslation()
                            .setContent("Jak często należy obracać poszkodowanego nieurazowego na drugi bok po ułożeniu go w pozycji bezpiecznej?")
                            .setLanguage(PL),
                    new QuestionTranslation()
                            .setContent("How often should you turn a non-traumatic victim to the other side after laying him in the recovery position?")
                            .setLanguage(EN),
                    new QuestionTranslation()
                            .setContent("Wie oft soll man einen symptomlosen Betroffenen auf die andere Körperseite nach dem Legen in stabiler Seitenlage drehen?")
                            .setLanguage(DE)
            );
        };
    }

}
