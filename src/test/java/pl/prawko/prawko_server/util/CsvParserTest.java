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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.exception.InvalidCsvException;
import pl.prawko.prawko_server.exception.UnknownCategoryException;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.testdata.QuestionCsvTestData;
import tools.jackson.databind.DatabindException;

import java.io.IOException;
import java.io.UncheckedIOException;
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

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CsvParserTest {

    // the columns that tests fill come first, so rows can leave out the rest
    private static final String HEADER = "Numer pytania,Poprawna odp,Liczba punktów,Pytanie,Odpowiedź A,Odpowiedź B,"
            + "Odpowiedź C,Media,Zakres struktury,Kategorie,Pytanie [EN],Odpowiedź A [EN],Odpowiedź B [EN],"
            + "Odpowiedź C [EN],Pytanie [D],Odpowiedź A [D],Odpowiedź B [D],Odpowiedź C [D]\n";

    @Mock
    private CsvFacade csvFacade;

    @InjectMocks
    private CsvParser csvParser;

    @Nested
    class Parse {

        @Test
        void returnQuestions_whenFileIsCsv() throws IOException {
            final var resource = new ClassPathResource(QuestionCsvTestData.CSV_FILE);
            final var file = new MockMultipartFile(
                    "file", QuestionCsvTestData.CSV_FILE, "text/csv", resource.getInputStream());
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
        void throwMultipartException_whenFileIsNotCsv() {
            final var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[]{});

            assertThatThrownBy(() -> csvParser.parse(file))
                    .isInstanceOf(MultipartException.class)
                    .hasMessage("Invalid file format.");
        }

        private static Stream<Arguments> malformedFiles() {
            return Stream.of(
                    Arguments.of(Named.of("invalid number", HEADER + "1,A,2\nabc,A,2\n"),
                            "Invalid CSV file at line 3 in column 'Numer pytania': "
                                    + "Cannot deserialize value of type `int` from String \"abc\": "
                                    + "not a valid `int` value"),
                    Arguments.of(Named.of("missing header column", HEADER.replace("Pytanie [EN],", "") + "1,A,2\n"),
                            "Invalid CSV file at line 2: Missing 1 header column: [\"Pytanie [EN]\"]"),
                    Arguments.of(Named.of("missing value", HEADER + "1,A\n"),
                            "Invalid CSV file at line 2 in column 'Liczba punktów': "
                                    + "Cannot map `null` into type `int` "
                                    + "(set `DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES` to 'false' to allow)"),
                    Arguments.of(Named.of("too many values", HEADER + "1,A,2" + ",".repeat(15) + ",extra\n"),
                            "Invalid CSV file at line 2: Too many entries: expected at most 18 (value #18 (5 chars) "
                                    + "\"extra\")"),
                    Arguments.of(Named.of("unclosed quote", HEADER + "\"1,A,2\n"),
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
        }

        @Test
        void throwInvalidCsvException_whenCategoriesAreUnknown(final CapturedOutput output) {
            final var content = HEADER + "1,A,2\n2,A,2,\"first\nsecond line\"\n3,A,2\n4,A,2\n";
            final var file = new MockMultipartFile(
                    "file", "categories.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
            when(csvFacade.mapSingleRow(any()))
                    .thenThrow(new UnknownCategoryException(List.of("Z")))
                    .thenReturn(new Question())
                    .thenThrow(new UnknownCategoryException(List.of("", "AA")))
                    .thenReturn(new Question());

            assertThatThrownBy(() -> csvParser.parse(file))
                    .isInstanceOf(InvalidCsvException.class)
                    .hasMessage("Invalid CSV file: line 2: unknown category 'Z'; line 5: unknown category ''; "
                            + "line 5: unknown category 'AA'. " + QuestionCsvTestData.ALLOWED_CATEGORIES);
            verify(csvFacade, times(4)).mapSingleRow(any());
            assertThat(output).contains("Invalid CSV file: 3 unknown categories. File: 'categories.csv'")
                    .doesNotContain("unknown category 'Z'");
        }

        @Test
        void throwInvalidCsvException_whenRowMappingFailsWithoutLocation() throws IOException {
            final var resource = new ClassPathResource(QuestionCsvTestData.CSV_FILE);
            final var file = new MockMultipartFile(
                    "file", QuestionCsvTestData.CSV_FILE, "text/csv", resource.getInputStream());
            when(csvFacade.mapSingleRow(any())).thenThrow(new DatabindException("Unknown category") {
            });

            assertThatThrownBy(() -> csvParser.parse(file))
                    .isInstanceOf(InvalidCsvException.class)
                    .hasMessage("Invalid CSV file: Unknown category");
        }

        @Test
        void throwUncheckedIOException_whenFileCannotBeRead() throws IOException {
            final var file = mock(MultipartFile.class);
            when(file.getContentType()).thenReturn("text/csv");
            final var cause = new IOException("Stream closed");
            when(file.getInputStream()).thenThrow(cause);

            assertThatThrownBy(() -> csvParser.parse(file))
                    .isExactlyInstanceOf(UncheckedIOException.class)
                    .hasMessage("CSV file failed to parse: Stream closed")
                    .cause()
                    .isSameAs(cause);
            verifyNoInteractions(csvFacade);
        }

    }

}
