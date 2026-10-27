package pl.prawko.prawko_server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pl.prawko.prawko_server.constants.ApiConstants;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Serves question media files (images and videos) from the {@code media.directory} directory as
 * {@code GET /media/{filename}}.
 *
 * <p>Spring's resource handler sets the content type from the file extension, answers range requests (so videos can be
 * streamed and seeked), conditional requests ({@code ETag}, {@code Last-Modified}) with {@code 304} and rejects paths
 * leading outside the directory. Responses are cacheable publicly for {@code media.cache-max-age}. A missing file
 * throws {@link org.springframework.web.servlet.resource.NoResourceFoundException}, answered with {@code 404}.
 *
 * <p>Media are public, like the question bank, so image loaders and video players need no credentials.
 */
@Configuration
public class MediaConfig implements WebMvcConfigurer {

    private final String location;
    private final Duration cacheMaxAge;

    public MediaConfig(@Value("${media.directory}") final Path directory,
                       @Value("${media.cache-max-age}") final Duration cacheMaxAge) {
        final var uri = directory.toAbsolutePath().normalize().toUri().toString();
        this.location = uri.endsWith("/") ? uri : uri + "/";
        this.cacheMaxAge = cacheMaxAge;
    }

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler(ApiConstants.MEDIA_BASE_URL_ALL)
                .addResourceLocations(location)
                .setCacheControl(CacheControl.maxAge(cacheMaxAge).cachePublic())
                .setEtagGenerator(MediaConfig::etag);
    }

    // like nginx: changes whenever the file is replaced, without reading its content
    private static String etag(final Resource resource) {
        try {
            return Long.toHexString(resource.lastModified()) + "-" + Long.toHexString(resource.contentLength());
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

}
