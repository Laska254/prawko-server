package pl.prawko.prawko_server.util;

import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.ExcelReaderException;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.exception.InvalidCsvException;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * Converts the {@value #SHEET} sheet of an XLSX workbook to CSV, so {@link CsvParser} imports it like an uploaded CSV
 * file; other sheets are ignored.
 * <p>
 * Every sheet row becomes one CSV row, also an empty or missing one, so row numbers in errors match the sheet. Rows are
 * cut to the width of the header (the first row). Text is written as is and numbers in plain notation
 * ({@code 14108}, {@code 3}), without the cell's display format.
 */
@Component
public class XlsxConverter {

    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String SHEET = "katalog";

    private static final Logger log = LoggerFactory.getLogger(XlsxConverter.class);
    private static final String[] EMPTY_ROW = {""};

    private final ObjectWriter csvWriter = new CsvMapper()
            .writerFor(String[].class)
            .with(CsvSchema.emptySchema()
                    .withLineSeparator("\n"));

    /**
     * Converts the workbook's {@value #SHEET} sheet to CSV.
     *
     * @param xlsx the workbook; not closed
     * @return the sheet as CSV, comma-separated with a header row
     * @throws InvalidCsvException if the input isn't an XLSX workbook or has no {@value #SHEET} sheet
     */
    public String toCsv(final InputStream xlsx) {
        try (final var workbook = new ReadableWorkbook(xlsx)) {
            final var sheet = workbook.findSheet(SHEET)
                    .orElseThrow(() -> {
                        log.warn("XLSX file has no sheet '{}'", SHEET);
                        return new InvalidCsvException("Invalid XLSX file: no sheet '" + SHEET + "'.");
                    });
            final var csv = new StringWriter();
            try (final var rows = sheet.openStream();
                 final var csvRows = csvWriter.writeValues(csv)) {
                final var iterator = rows.iterator();
                int nextRow = 1;
                int width = 0;
                while (iterator.hasNext()) {
                    final var row = iterator.next();
                    for (; nextRow < row.getRowNum(); nextRow++) {
                        csvRows.write(EMPTY_ROW);
                    }
                    if (width == 0) {
                        width = row.getCellCount();
                    }
                    csvRows.write(values(row, width));
                    nextRow++;
                }
            }
            return csv.toString();
        } catch (final IOException | ExcelReaderException exception) {
            // a file that isn't a ZIP archive also fails with an IOException
            log.warn("Invalid XLSX file: {}", exception.getMessage());
            throw new InvalidCsvException("Invalid XLSX file.");
        }
    }

    private static String[] values(final Row row, final int width) {
        // at least one value: a row without any writes nothing, not an empty line
        final var values = new String[Math.max(1, Math.min(row.getCellCount(), width))];
        for (int index = 0; index < values.length; index++) {
            values[index] = row.getOptionalCell(index)
                    .map(XlsxConverter::text)
                    .orElse("");
        }
        return values;
    }

    private static String text(final Cell cell) {
        if (cell.getType() == CellType.NUMBER) {
            return cell.asNumber()
                    .stripTrailingZeros()
                    .toPlainString();
        }
        return cell.getText();
    }

}
