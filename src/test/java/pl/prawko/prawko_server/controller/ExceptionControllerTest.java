package pl.prawko.prawko_server.controller;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExceptionControllerTest {

    private final ExceptionController controller = new ExceptionController();

    private static Stream<Arguments> userConstraintViolations() {
        return Stream.of(
                Arguments.of(Named.of("username on H2", "PUBLIC.UK_USER_USER_NAME INDEX PUBLIC.UK_USER_USER_NAME_INDEX_3"),
                        "userName", "User with this username already exists."),
                Arguments.of(Named.of("email on MariaDB", "uk_user_email"),
                        "email", "User with this email already exists.")
        );
    }

    private static Stream<Arguments> otherViolations() {
        return Stream.of(
                Arguments.of(Named.of("other constraint", violation("PUBLIC.CONSTRAINT_3D3 INDEX PUBLIC.CONSTRAINT_INDEX_3D"))),
                Arguments.of(Named.of("constraint only starting with a user constraint", violation("uk_user_email_old"))),
                Arguments.of(Named.of("unknown constraint", violation(null))),
                Arguments.of(Named.of("not a constraint violation", new DataIntegrityViolationException("Value too long")))
        );
    }

    private static DataIntegrityViolationException violation(final String constraintName) {
        return new DataIntegrityViolationException("Constraint violated",
                new ConstraintViolationException("Constraint violated", new SQLException(), constraintName));
    }

    @Test
    void handleConcurrentModification_returnConflict() {
        final var result = controller.handleConcurrentModification();

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getDetail()).isEqualTo("Resource was modified concurrently, try again.");
    }

    @ParameterizedTest
    @MethodSource("userConstraintViolations")
    void handleDataIntegrityViolation_returnConflictWithField_whenUserConstraintIsViolated(final String constraintName,
                                                                                         final String field,
                                                                                         final String message) {
        final var result = controller.handleDataIntegrityViolation(violation(constraintName));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getDetail()).isEqualTo(UserTestData.USER_ALREADY_EXISTS);
        assertThat(result.getProperties()).containsEntry("details", Map.ofEntries(Map.entry(field, message)));
    }

    @ParameterizedTest
    @MethodSource("otherViolations")
    void handleDataIntegrityViolation_rethrow_whenNoUserConstraintIsViolated(final DataIntegrityViolationException exception) {
        assertThatThrownBy(() -> controller.handleDataIntegrityViolation(exception))
                .isSameAs(exception);
    }

}
