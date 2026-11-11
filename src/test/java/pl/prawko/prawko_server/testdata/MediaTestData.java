package pl.prawko.prawko_server.testdata;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class MediaTestData {

    public static final String CATALOGUE_FILENAMES =
            "pl.prawko.prawko_server.testdata.MediaTestData#catalogueFilenames";

    private MediaTestData() {
    }

    /**
     * Media filenames shaped like the ones in the 07.2026 catalogue (as images), each with its encoded URL path
     * segment.
     */
    public static Stream<Arguments> catalogueFilenames() {
        return Stream.of(
                Arguments.of("JAZDA NOCĄorg.jpg", "JAZDA%20NOC%C4%84org.jpg"),
                Arguments.of("zagłówekorg.jpg", "zag%C5%82%C3%B3wekorg.jpg"),
                Arguments.of("W4(23,54,80)org.jpg", "W4(23,54,80)org.jpg"),
                Arguments.of("A-4 (5)org.jpg", "A-4%20(5)org.jpg"),
                Arguments.of("!RS_Droga bez przejazdu.jpg", "!RS_Droga%20bez%20przejazdu.jpg"),
                Arguments.of("Znak D1+T6 w prawo.jpg", "Znak%20D1+T6%20w%20prawo.jpg"),
                Arguments.of("Klip7 00_00_05-00_00_13~1.jpg", "Klip7%2000_00_05-00_00_13~1.jpg"),
                // decomposed: letters followed by combining accents, as some catalogue names are written
                Arguments.of("Ostrożność pkt 8.jpg", "Ostroz%CC%87nos%CC%81c%CC%81%20pkt%208.jpg"));
    }

}
