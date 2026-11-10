package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that {@code prod} brings in the {@code mariadb} profile that {@code UserRepositoryMariaDbTest} activates
 * directly. Starts an empty context, so the {@code prod} placeholders stay unresolved and need no env vars.
 */
class ProdProfileTest {

    @Test
    void prodProfile_includeMariaDbMapping() {
        try (final var context = new SpringApplicationBuilder(Empty.class)
                .web(WebApplicationType.NONE)
                .profiles("prod")
                .run()) {
            final var mappingResources = context.getEnvironment()
                    .getProperty("spring.jpa.mapping-resources", String[].class);
            assertThat(mappingResources).contains("META-INF/orm-mariadb.xml");
        }
    }

    // Not a @Configuration, so the application's component scan doesn't pick it up.
    static class Empty {
    }

}
