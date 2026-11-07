package pl.prawko.prawko_server.exception;

import java.util.List;

/**
 * Custom exception to throw when category names don't match any stored category.
 * Carries every unknown name, an empty string for an empty one.
 */
public class UnknownCategoryException extends RuntimeException {

    private final List<String> names;

    public UnknownCategoryException(final List<String> names) {
        super("Unknown categories: " + names);
        this.names = names;
    }

    public List<String> getNames() {
        return names;
    }

}
