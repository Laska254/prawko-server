package pl.prawko.prawko_server.testdata;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadFeature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class XlsxTestData {

    public static final String UNDER_VERIFICATION_SHEET = "W trakcie weryfikacji";
    public static final String INVALID_XLSX = "Invalid XLSX file.";
    public static final String NO_KATALOG_SHEET = "Invalid XLSX file: no sheet 'katalog'.";

    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d+");

    private XlsxTestData() {
    }

    /**
     * Writes a workbook with a sheet per entry. A {@link Number} makes a number cell, {@code null} leaves the cell out
     * and an empty row leaves the row out.
     */
    public static byte[] createWorkbook(final Map<String, List<List<Object>>> sheets) throws IOException {
        final var output = new ByteArrayOutputStream();
        try (final var workbook = new Workbook(output, "test", "1.0")) {
            sheets.forEach((name, rows) -> write(workbook.newWorksheet(name), rows));
        }
        return output.toByteArray();
    }

    /**
     * Reads a CSV file from the classpath into rows, with whole numbers as numbers, as the catalogue stores some IDs.
     */
    public static List<List<Object>> readCsvRows(final String path) throws IOException {
        try (final var input = new ClassPathResource(path).getInputStream();
             final MappingIterator<String[]> rows = new CsvMapper()
                     .readerFor(String[].class)
                     .with(CsvReadFeature.WRAP_AS_ARRAY)
                     .readValues(input)) {
            return rows.readAll().stream()
                    .map(row -> Arrays.stream(row)
                            .map(XlsxTestData::toCell)
                            .toList())
                    .toList();
        }
    }

    private static void write(final Worksheet sheet, final List<List<Object>> rows) {
        for (int row = 0; row < rows.size(); row++) {
            final var values = rows.get(row);
            for (int column = 0; column < values.size(); column++) {
                switch (values.get(column)) {
                    case null -> {
                    }
                    case final Number number -> sheet.value(row, column, number);
                    case final Object value -> sheet.value(row, column, value.toString());
                }
            }
        }
    }

    private static Object toCell(final String value) {
        if (WHOLE_NUMBER.matcher(value).matches()) {
            return Long.valueOf(value);
        }
        return value;
    }

}
