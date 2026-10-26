package pl.prawko.prawko_server.model;

import java.util.Arrays;
import java.util.Optional;

public enum CategoryVariant {

    A,
    A1,
    A2,
    AM,
    B,
    B1,
    C,
    C1,
    D,
    D1,
    T,
    PT;

    /**
     * Returns the {@code CategoryVariant} with exactly the given name.
     *
     * @param name the name of the category, e.g. {@code PT}
     * @return the matching {@code CategoryVariant}, or empty if no variant has this name
     */
    public static Optional<CategoryVariant> ofName(final String name) {
        return Arrays.stream(values())
                .filter(variant -> variant.name().equals(name))
                .findFirst();
    }

}
