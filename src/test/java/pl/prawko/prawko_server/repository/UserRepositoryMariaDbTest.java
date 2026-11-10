package pl.prawko.prawko_server.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;
import pl.prawko.prawko_server.controller.ExceptionController;
import pl.prawko.prawko_server.model.EmailStatus;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.service.IIdentityNotificationService;
import pl.prawko.prawko_server.service.IUserIdentityService;
import pl.prawko.prawko_server.service.implementation.UserIdentityService;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Runs what depends on the database on MariaDB, the production database, with the {@code mariadb} profile of
 * {@code prod}: the constraint names behind the {@code 409} details, the collation of the unique columns and the
 * provider sign-in relying on it. Skipped without Docker; CI deactivates that condition, so there a missing Docker fails
 * the build. The container is a bean, so it stops with the cached context, after the connection pool, not after this
 * class.
 */
@DataJpaTest
@ActiveProfiles({"test", "mariadb"})
@Import(UserIdentityService.class)
@Testcontainers(disabledWithoutDocker = true)
class UserRepositoryMariaDbTest {

    @Autowired
    private UserRepository repository;

    @Autowired
    private IUserIdentityService userIdentityService;

    @MockitoBean
    private IIdentityNotificationService notificationService;

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

    @Test
    void saveAndFlush_saveUser_whenEmailDiffersOnlyInAccents() {
        final var merry = UserTestData.createMerry().setEmail(UserTestData.ACCENTED_EMAIL);
        assertThatNoException().isThrownBy(() -> repository.saveAndFlush(merry));
    }

    @Test
    void findByEmailIgnoreCase_returnUser_whenCaseDiffers() {
        final var result = repository.findByEmailIgnoreCase(tester.getEmail().toUpperCase(Locale.ROOT));
        assertThat(result).get().extracting(User::getId).isEqualTo(tester.getId());
    }

    @Test
    void findByEmailIgnoreCase_returnEmpty_whenEmailDiffersOnlyInAccents() {
        final var result = repository.findByEmailIgnoreCase(UserTestData.ACCENTED_EMAIL);
        assertThat(result).isEmpty();
    }

    @Test
    void existsByEmailIgnoreCase_returnFalse_whenEmailDiffersOnlyInAccents() {
        final var result = repository.existsByEmailIgnoreCase(UserTestData.ACCENTED_EMAIL);
        assertThat(result).isFalse();
    }

    @Test
    void resolveUser_createUser_whenEmailDiffersOnlyInAccents() {
        final var profile = UserTestData.createPippinProfile(IdentityProvider.GOOGLE, UserTestData.ACCENTED_EMAIL,
                EmailStatus.AUTHORITATIVE);

        final var result = userIdentityService.resolveUser(profile);

        assertThat(result.getId()).isNotEqualTo(tester.getId());
        assertThat(result.getEmail()).isEqualTo(UserTestData.ACCENTED_EMAIL);
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
