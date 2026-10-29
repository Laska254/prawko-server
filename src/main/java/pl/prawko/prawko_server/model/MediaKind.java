package pl.prawko.prawko_server.model;

import org.springframework.util.MimeType;

import java.util.Arrays;
import java.util.Optional;

/**
 * Kind of media attached to a question, so clients know whether to show an image or play a video without guessing
 * from the file extension.
 */
public enum MediaKind {

    IMAGE("image"),
    VIDEO("video");

    private final String mimeType;

    MediaKind(final String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Returns the kind matching the top-level type of the given MIME type.
     *
     * @param mimeType the content type of a media file, e.g. {@code image/jpeg}
     * @return the matching {@code MediaKind}, or empty if it's neither an image nor a video
     */
    public static Optional<MediaKind> of(final MimeType mimeType) {
        return Arrays.stream(values())
                .filter(kind -> kind.mimeType.equals(mimeType.getType()))
                .findFirst();
    }

}
