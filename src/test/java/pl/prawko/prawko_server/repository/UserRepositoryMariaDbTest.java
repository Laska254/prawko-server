package pl.prawko.prawko_server.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;
import pl.prawko.prawko_server.controller.ExceptionController;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Runs what depends on the database on MariaDB, the production database: the constraint names behind the {@code 409}
 * details and the collation of the unique columns. Skipped without Docker; CI deactivates that condition, so there a
 * missing Docker fails the build. The container is a bean, so it stops with the cached context, after the connection
 * pool, not after this class.
 */
@DataJpaTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class UserRepositoryMariaDbTest {

    @Autowired
    private UserRepository repository;

    private User tester;

    @BeforeEach
    void setUp() {
        tester = repository.save(UserTestData.createTestUserPippin());
    }

    @Test
    void saveAndFlush_throwDataIntegrityViolationHandledAsConflict_whenUserNameIsTaken() {
        final var merry = UserTestData.createMerry().setUserName(tester.getUserName());

        final var exception = catchThrowableOfType(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(merry));

        final var problem = new ExceptionController().handleDataIntegrityViolation(exception);
        assertThat(problem.getProperties())
                .containsEntry("details", Map.ofEntries(Map.entry("userName", UserTestData.USER_NAME_TAKEN)));
    }

    @Test
    void saveAndFlush_throwDataIntegrityViolationHandledAsConflict_whenEmailIsTaken() {
        final var merry = UserTestData.createMerry().setEmail(tester.getEmail());

        final var exception = catchThrowableOfType(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(merry));

        final var problem = new ExceptionController().handleDataIntegrityViolation(exception);
        assertThat(problem.getProperties())
                .containsEntry("details", Map.ofEntries(Map.entry("email", UserTestData.EMAIL_TAKEN)));
    }

    @Test
    void saveAndFlush_throwDataIntegrityViolationHandledAsConflict_whenUserNameDiffersOnlyInAccents() {
        repository.saveAndFlush(UserTestData.createMerry().setUserName("zaba"));
        final var accented = UserTestData.createTestUserPippin()
                .setEmail("zaba@shire.me")
                .setUserName("żaba");

        final var exception = catchThrowableOfType(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(accented));

        final var problem = new ExceptionController().handleDataIntegrityViolation(exception);
        assertThat(problem.getProperties())
                .containsEntry("details", Map.ofEntries(Map.entry("userName", UserTestData.USER_NAME_TAKEN)));
    }

    @Test
    void existsByUserNameIgnoreCase_returnTrue_whenUserNameDiffersOnlyInAccents() {
        final var result = repository.existsByUserNameIgnoreCase(UserTestData.ACCENTED_USER_NAME);
        assertThat(result).isTrue();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MariaDbConfig {

        @Bean
        @ServiceConnection
        MariaDBContainer mariaDb() {
            return new MariaDBContainer("mariadb:11.8");
        }

    }

}
