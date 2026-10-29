package pl.prawko.prawko_server.mapper;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import pl.prawko.prawko_server.model.MediaKind;

/**
 * Maps a question's media filename to what clients need to show it, used by {@link QuestionMapper}.
 * <p>
 * The URL is the filename appended to {@code media.base-url}, so serving media from a CDN instead of
 * {@link pl.prawko.prawko_server.config.MediaConfig} only changes that property. The {@link MediaKind} comes from the
 * content type of the file extension, the same one the media are served with. A question without media (blank
 * filename) maps to {@code null}; the kind is also {@code null} when the extension is neither an image nor a video.
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
        return UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment(filename)
                .toUriString();
    }

    @Named("mediaType")
    public @Nullable MediaKind toType(final @Nullable String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        return MediaTypeFactory.getMediaType(filename)
                .flatMap(MediaKind::of)
                .orElse(null);
    }

}
