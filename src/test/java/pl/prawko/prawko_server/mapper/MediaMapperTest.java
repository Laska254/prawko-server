package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pl.prawko.prawko_server.model.MediaKind;
import pl.prawko.prawko_server.test_data.QuestionTestData;

import static org.assertj.core.api.Assertions.assertThat;

class MediaMapperTest {

    private final MediaMapper mediaMapper = new MediaMapper(QuestionTestData.MEDIA_BASE_URL);

    @Nested
    class ToUrl {

        @ParameterizedTest
        @ValueSource(strings = {QuestionTestData.MEDIA_BASE_URL, QuestionTestData.MEDIA_BASE_URL + "/"})
        void appendFilenameToBaseUrl_whenFilenameIsSet(final String baseUrl) {
            final var result = new MediaMapper(baseUrl).toUrl("R_101org.jpg");

            assertThat(result).isEqualTo(QuestionTestData.MEDIA_BASE_URL + "/R_101org.jpg");
        }

        @Test
        void encodeFilename_whenFilenameHasSpecialCharacters() {
            final var result = mediaMapper.toUrl("znak drogowy#1.jpg");

            assertThat(result).isEqualTo(QuestionTestData.MEDIA_BASE_URL + "/znak%20drogowy%231.jpg");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = " ")
        void returnNull_whenFilenameIsBlank(final String filename) {
            assertThat(mediaMapper.toUrl(filename)).isNull();
        }

    }

    @Nested
    class ToType {

        @ParameterizedTest
        @CsvSource({
                "R_101org.jpg, IMAGE",
                "R_101org.png, IMAGE",
                "AK_D11_45org.webm, VIDEO",
                "AK_D11_45org.wmv, VIDEO"
        })
        void mapByExtension_whenFileIsImageOrVideo(final String filename, final MediaKind expected) {
            assertThat(mediaMapper.toType(filename)).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(strings = {"notes.txt", "R_101org.unknown", "R_101org"})
        void returnNull_whenFileIsNeitherImageNorVideo(final String filename) {
            assertThat(mediaMapper.toType(filename)).isNull();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = " ")
        void returnNull_whenFilenameIsBlank(final String filename) {
            assertThat(mediaMapper.toType(filename)).isNull();
        }

    }

}
