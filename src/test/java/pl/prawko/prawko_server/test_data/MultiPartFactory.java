package pl.prawko.prawko_server.test_data;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.unit.DataSize;

public class MultiPartFactory {

    private static final String PART_NAME = "file";

    public static MultiValueMap<String, Object> empty() {
        return new LinkedMultiValueMap<>();
    }

    public static MultiValueMap<String, Object> withWrongFile() {
        return withFile(new byte[0], "wrong-file.txt");
    }

    public static MultiValueMap<String, Object> withCsvOfSize(final DataSize size) {
        return withFile(new byte[Math.toIntExact(size.toBytes())], "questions.csv");
    }

    public static MultiValueMap<String, Object> fromClasspath(final String path) {
        final var parts = empty();
        parts.add(PART_NAME, new ClassPathResource(path));
        return parts;
    }

    private static MultiValueMap<String, Object> withFile(final byte[] content, final String filename) {
        final var resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        final var parts = empty();
        parts.add(PART_NAME, resource);
        return parts;
    }

}
