package pl.prawko.prawko_server.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryVariantTest {

    @ParameterizedTest
    @EnumSource(CategoryVariant.class)
    void ofName_returnVariant_whenNameMatches(final CategoryVariant variant) {
        assertThat(CategoryVariant.ofName(variant.name())).contains(variant);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Z", "b", "P", ""})
    void ofName_returnEmpty_whenNameDoesNotMatchExactly(final String name) {
        assertThat(CategoryVariant.ofName(name)).isEmpty();
    }

}
