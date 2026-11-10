package pl.prawko.prawko_server.testdata;

import pl.prawko.prawko_server.model.Language;

import java.util.List;

public class LanguageTestData {

    public static final Language PL = new Language()
            .setId(1L)
            .setName("polish")
            .setCode("pl");
    public static final Language EN = new Language()
            .setId(2L)
            .setName("english")
            .setCode("en");
    public static final Language DE = new Language()
            .setId(3L)
            .setName("german")
            .setCode("de");

    public static final List<Language> ALL = List.of(
            PL, EN, DE
    );

}
