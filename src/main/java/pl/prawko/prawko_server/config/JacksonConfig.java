package pl.prawko.prawko_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson JSON processing configuration.
 */
@Configuration
public class JacksonConfig {

    /**
     * Creates and configures the global ObjectMapper bean.
     *
     * <p>The mapper is configured with {@link JavaTimeModule}.
     *
     * @return configured {@link ObjectMapper} instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
    }

}
