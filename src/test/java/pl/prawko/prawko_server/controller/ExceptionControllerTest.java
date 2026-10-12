package pl.prawko.prawko_server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionControllerTest {

    private final ExceptionController controller = new ExceptionController();

    @Test
    void handleConcurrentModification_returnConflict() {
        final var result = controller.handleConcurrentModification();

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getDetail()).isEqualTo("Resource was modified concurrently, try again.");
    }

}
