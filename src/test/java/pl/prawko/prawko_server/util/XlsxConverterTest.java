package pl.prawko.prawko_server.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.prawko.prawko_server.exception.InvalidQuestionFileException;
import pl.prawko.prawko_server.testdata.XlsxTestData;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XlsxConverterTest {

    private final XlsxConverter converter = new XlsxConverter();

    @Nested
    class ToCsv {

        @Test
        void returnKatalogSheetAsCsv_whenWorkbookIsValid() throws IOException {
            final var workbook = XlsxTestData.createWorkbook(Map.ofEntries(
                    Map.entry(XlsxConverter.SHEET, List.of(
                            List.of("Lp", "Numer pytania", "Pytanie"),
                            List.of(1, 99, "Czy na pewno, \"tak\"\nczy nie?"),
                            List.of(),
                            List.of(3, 100.0, "Ostatnie", "poza nagłówkiem"),
                            Arrays.asList(4, null, null))),
                    Map.entry(XlsxTestData.UNDER_VERIFICATION_SHEET, List.of(
                            List.of("Lp", "Numer pytania"),
                            List.of(1, 500)))));

            final var result = converter.toCsv(new ByteArrayInputStream(workbook));

            assertThat(result).isEqualTo("""
                    Lp,"Numer pytania",Pytanie
                    1,99,"Czy na pewno, ""tak""
                    czy nie?"

                    3,100,Ostatnie
                    4,,
                    """);
        }

        @Test
        void padRowWithEmptyValues_whenItsLastCellsAreAbsent() throws IOException {
            final var workbook = XlsxTestData.createWorkbook(Map.ofEntries(
                    Map.entry(XlsxConverter.SHEET, List.of(
                            List.of("Numer pytania", "Pytanie", "Media"),
                            List.of(99, "Bez mediów")))));

            final var result = converter.toCsv(new ByteArrayInputStream(workbook));

            assertThat(result).isEqualTo("""
                    "Numer pytania",Pytanie,Media
                    99,"Bez mediów",
                    """);
        }

        @Test
        void throwInvalidQuestionFileException_whenFileIsNotXlsx() {
            final var input = new ByteArrayInputStream("Lp,Numer pytania\n1,99\n".getBytes(StandardCharsets.UTF_8));

            assertThatThrownBy(() -> converter.toCsv(input))
                    .isInstanceOf(InvalidQuestionFileException.class)
                    .hasMessage("Invalid XLSX file.");
        }

        @Test
        void throwInvalidQuestionFileException_whenSheetPartIsMissing(@TempDir final Path directory)
                throws IOException {
            final var workbook = directory.resolve("workbook.xlsx");
            Files.write(workbook, XlsxTestData.createWorkbook(Map.ofEntries(
                    Map.entry(XlsxConverter.SHEET, List.of(List.of("Lp"))))));
            try (final var zip = FileSystems.newFileSystem(workbook)) {
                Files.delete(zip.getPath("xl/worksheets/sheet1.xml"));
            }
            final var broken = Files.readAllBytes(workbook);

            assertThatThrownBy(() -> converter.toCsv(new ByteArrayInputStream(broken)))
                    .isInstanceOf(InvalidQuestionFileException.class)
                    .hasMessage("Invalid XLSX file.");
        }

        @Test
        void throwInvalidQuestionFileException_whenKatalogSheetIsMissing() throws IOException {
            final var workbook = XlsxTestData.createWorkbook(Map.ofEntries(
                    Map.entry(XlsxTestData.UNDER_VERIFICATION_SHEET, List.of(List.of("Lp")))));

            assertThatThrownBy(() -> converter.toCsv(new ByteArrayInputStream(workbook)))
                    .isInstanceOf(InvalidQuestionFileException.class)
                    .hasMessage("Invalid XLSX file: no sheet 'katalog'.");
        }

    }

}
