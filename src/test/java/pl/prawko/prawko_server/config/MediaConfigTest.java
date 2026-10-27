package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import pl.prawko.prawko_server.constants.ApiConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@IntegrationTest
class MediaConfigTest {

    private static final String IMAGE = "test_image.jpg";
    private static final String VIDEO = "test_video.mp4";

    @Value("${media.directory}")
    private Path directory;

    @LocalServerPort
    private int port;

    private RestTestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = TestUtils.createRestTestClient(port, ApiConstants.MEDIA_BASE_URL);
    }

    @Test
    void getMedia_returnFileWithCacheHeaders_whenFileExists() throws IOException {
        final var content = Files.readAllBytes(directory.resolve(IMAGE));

        restClient.get()
                .uri("/" + IMAGE)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_JPEG)
                .expectHeader().valueEquals(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                .expectHeader().exists(HttpHeaders.ETAG)
                .expectHeader().exists(HttpHeaders.LAST_MODIFIED)
                .expectHeader().valueEquals(HttpHeaders.ACCEPT_RANGES, "bytes")
                .expectBody(byte[].class).isEqualTo(content);
    }

    @Test
    void getMedia_returnPartialContent_whenRangeIsRequested() throws IOException {
        final var content = Files.readAllBytes(directory.resolve(VIDEO));

        restClient.get()
                .uri("/" + VIDEO)
                .header(HttpHeaders.RANGE, "bytes=0-9")
                .exchange()
                .expectStatus().isEqualTo(206)
                .expectHeader().contentType("video/mp4")
                .expectHeader().valueEquals(HttpHeaders.CONTENT_RANGE, "bytes 0-9/" + content.length)
                .expectBody(byte[].class).isEqualTo(Arrays.copyOf(content, 10));
    }

    @Test
    void getMedia_returnNotModified_whenEtagMatches() {
        final var etag = restClient.get()
                .uri("/" + IMAGE)
                .exchange()
                .returnResult()
                .getResponseHeaders()
                .getETag();

        restClient.get()
                .uri("/" + IMAGE)
                .header(HttpHeaders.IF_NONE_MATCH, etag)
                .exchange()
                .expectStatus().isNotModified();
    }

    @Test
    void getMedia_returnNotFound_whenFileIsMissing() {
        restClient.get()
                .uri("/missing.jpg")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.detail").isEqualTo("File not found.");
    }

    // Tomcat normalizes "/media/../x" to "/x" (denied) and rejects encoded separators, the handler rejects ".." too
    @ParameterizedTest
    @ValueSource(strings = {"/../application-test.yaml", "/%2e%2e/application-test.yaml", "/..%2fapplication-test.yaml"})
    void getMedia_returnClientError_whenPathLeavesDirectory(final String path) {
        restClient.get()
                .uri(path)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().doesNotExist(HttpHeaders.ETAG);
    }

}
