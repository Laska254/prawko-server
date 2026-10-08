package pl.prawko.prawko_server.util;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.exception.InvalidCsvException;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.test_data.QuestionCSVTestData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CSVParserTest {

    private static final String HEADER = "Nazwa pytania,Numer pytania,Poprawna odp,Liczba punktów\n";

    @Mock
    private CsvFacade csvFacade;

    @InjectMocks
    private CSVParser csvParser;

    @Nested
    class Parse {

        @Test
        void returnQuestions_whenFileIsCSV() throws IOException {
            final var resource = new ClassPathResource(QuestionCSVTestData.CSV_FILE);
            final var file = new MockMultipartFile(
                    "file", QuestionCSVTestData.CSV_FILE, "text/csv", resource.getInputStream());
            final var question1 = new Question();
            final var question2 = new Question();
            final var expected = List.of(question1, question2);

            when(csvFacade.mapSingleRow(any()))
                    .thenReturn(question1)
                    .thenReturn(question2);

            final var result = csvParser.parse(file);

            assertThat(result).isEqualTo(expected);
            verify(csvFacade, times(2)).mapSingleRow(any());
            verifyNoMoreInteractions(csvFacade);
        }

        @Test
        void throwMultipartException_whenFileIsNotCSV() {
            final var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[]{});

            assertThatThrownBy(() -> csvParser.parse(file))
                    .isInstanceOf(MultipartException.class)
                    .hasMessage("Invalid file format.");
        }

        private static Stream<Arguments> malformedFiles() {
            return Stream.of(
                    Arguments.of(Named.of("invalid number", HEADER + "W1,1,A,2\nW2,abc,A,2\n"),
                            "Invalid CSV file at line 3 in column 'Numer pytania': "
                                    + "Cannot deserialize value of type `int` from String \"abc\": not a valid `int` value"),
                    Arguments.of(Named.of("missing required column", "Nazwa pytania\nW1\n"),
                            "Invalid CSV file at line 2 in column 'Numer pytania': "
                                    + "Cannot map `null` into type `int` "
                                    + "(set `DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES` to 'false' to allow)"),
                    Arguments.of(Named.of("too many values", HEADER + "W1,1,A,2,extra\n"),
                            "Invalid CSV file at line 2: Too many entries: expected at most 4 (value #4 (5 chars) \"extra\")"),
                    Arguments.of(Named.of("unclosed quote", HEADER + "\"W1,1,A,2\n"),
                            "Invalid CSV file at line 3: Missing closing quote for value"),
                    Arguments.of(Named.of("empty file", ""),
                            "Invalid CSV file at line 1: Empty header line: can not bind data"));
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("malformedFiles")
        void throwInvalidCsvException_whenContentIsMalformed(final String content, final String expectedMessage) {
            final var file = new MockMultipartFile(
                    "file", "malformed.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

            assertThatThrownBy(() -> csvParser.parse(file))
                    .isInstanceOf(InvalidCsvException.class)
                    .hasMessage(expectedMessage);
            verifyNoInteractions(csvFacade);
        }

        @Test
        void throwRuntimeException_whenFileCannotBeRead() throws IOException {
            final var file = mock(MultipartFile.class);
            when(file.getContentType()).thenReturn("text/csv");
            when(file.getInputStream()).thenThrow(new IOException("Stream closed"));

            assertThatThrownBy(() -> csvParser.parse(file))
                    .isExactlyInstanceOf(RuntimeException.class)
                    .hasMessage("CSV file failed to parse: Stream closed");
            verifyNoInteractions(csvFacade);
        }

    }

}