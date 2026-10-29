package pl.prawko.prawko_server.mapper;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import pl.prawko.prawko_server.model.MediaType;

/**
 * Maps a question's media filename to what clients need to show it, used by {@link QuestionMapper}.
 * <p>
 * The URL is the filename appended to {@code media.base-url}, so serving media from a CDN instead of
 * {@link pl.prawko.prawko_server.config.MediaConfig} only changes that property. The {@link MediaType} comes from the
 * content type of the file extension, the same one the media are served with. A question without media (blank
 * filename) maps to {@code null}.
 */
@Component
public class MediaMapper {

    private final String baseUrl;

    public MediaMapper(@Value("${media.base-url}") final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Named("mediaUrl")
    public @Nullable String toUrl(final @Nullable String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        return UriComponentsBuilder.fromUriString(baseUrl).pathSegment(filename).toUriString();
    }

    @Named("mediaType")
    public @Nullable MediaType toType(final @Nullable String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        return MediaTypeFactory.getMediaType(filename)
                .map(contentType -> switch (contentType.getType()) {
                    case "image" -> MediaType.IMAGE;
                    case "video" -> MediaType.VIDEO;
                    default -> null;
                })
                .orElse(null);
    }

}
