package pl.prawko.prawko_server.config;

import org.springframework.data.web.PagedModel;

import java.util.List;

/**
 * Test-side representation of a serialized {@link PagedModel}.
 */
public record PageResponse<T>(List<T> content, PagedModel.PageMetadata page) {
}
