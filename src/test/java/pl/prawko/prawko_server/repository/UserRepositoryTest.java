package pl.prawko.prawko_server.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import pl.prawko.prawko_server.controller.ExceptionController;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.model.UserIdentity;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    private final String wrongUserName = "nonExistingUserName";
    private final String wrongEmail = "nonExistingEmail";
    private final String tokenHash = "a".repeat(64);

    @Autowired
    private UserRepository repository;

    @Autowired
    private EntityManager entityManager;

    private User tester;

    @BeforeEach
    void setUp() {
        tester = repository.save(UserTestData.createTestUserPippin());
    }

    @Test
    void existsByUserNameIgnoreCase_returnTrue_whenUserNameExists() {
        final var result = repository.existsByUserNameIgnoreCase(tester.getUserName());
        assertThat(result).isTrue();
    }

    @Test
    void existsByUserNameIgnoreCase_returnFalse_whenUserNameDoesNotExist() {
        final var result = repository.existsByUserNameIgnoreCase(wrongUserName);
        assertThat(result).isFalse();
    }

    @Test
    void existsByEmailIgnoreCase_returnTrue_whenEmailExists() {
        final var result = repository.existsByEmailIgnoreCase(tester.getEmail());
        assertThat(result).isTrue();
    }

    @Test
    void existsByEmailIgnoreCase_returnFalse_whenEmailDoesNotExist() {
        final var result = repository.existsByEmailIgnoreCase(wrongEmail);
        assertThat(result).isFalse();
    }

    @Test
    void findByUserNameOrEmailAllIgnoreCase_returnUser_whenFoundByUserName() {
        final var result = repository.findByUserNameOrEmailAllIgnoreCase(tester.getUserName(), tester.getUserName());
        assertThat(result.get().getUserName()).isEqualTo(tester.getUserName());
    }

    @Test
    void findByUserNameOrEmailAllIgnoreCase_returnUser_whenFoundByEmail() {
        final var result = repository.findByUserNameOrEmailAllIgnoreCase(tester.getEmail(), tester.getEmail());
        assertThat(result.get().getEmail()).isEqualTo(tester.getEmail());
    }

    @Test
    void findByUserNameOrEmailAllIgnoreCase_returnEmpty_whenNotFoundByUserName() {
        final var result = repository.findByUserNameOrEmailAllIgnoreCase(wrongUserName, wrongUserName);
        assertThat(result).isEmpty();
    }

    @Test
    void findByUserNameOrEmailAllIgnoreCase_returnEmpty_whenNotFoundByEmail() {
        final var result = repository.findByUserNameOrEmailAllIgnoreCase(wrongEmail, wrongEmail);
        assertThat(result).isEmpty();
    }

    @Test
    void findByEmailIgnoreCase_returnUser_whenEmailCaseDiffers() {
        final var result = repository.findByEmailIgnoreCase(tester.getEmail().toUpperCase());
        assertThat(result).get().extracting(User::getId).isEqualTo(tester.getId());
    }

    @Test
    void findByEmailIgnoreCase_returnEmpty_whenEmailDoesNotExist() {
        final var result = repository.findByEmailIgnoreCase(wrongEmail);
        assertThat(result).isEmpty();
    }

    @Test
    void findByEmailIgnoreCase_returnEmpty_whenGivenUserName() {
        final var result = repository.findByEmailIgnoreCase(tester.getUserName());
        assertThat(result).isEmpty();
    }

    @Test
    void findByPasswordResetTokenHash_returnUser_whenHashMatches() {
        repository.save(tester.setPasswordResetTokenHash(tokenHash));

        final var result = repository.findByPasswordResetTokenHash(tokenHash);

        assertThat(result).get().extracting(User::getId).isEqualTo(tester.getId());
    }

    @Test
    void findByPasswordResetTokenHash_returnEmpty_whenNoTokenIssued() {
        final var result = repository.findByPasswordResetTokenHash(tokenHash);
        assertThat(result).isEmpty();
    }

    @Test
    void findByVerificationOrCurrentEmailTokenHash_returnUser_whenVerificationHashMatches() {
        repository.save(tester.setVerificationTokenHash(tokenHash));

        final var result = repository.findByVerificationOrCurrentEmailTokenHash(tokenHash);

        assertThat(result).get().extracting(User::getId).isEqualTo(tester.getId());
    }

    @Test
    void findByVerificationOrCurrentEmailTokenHash_returnUser_whenCurrentEmailHashMatches() {
        repository.save(tester.setCurrentEmailTokenHash(tokenHash));

        final var result = repository.findByVerificationOrCurrentEmailTokenHash(tokenHash);

        assertThat(result).get().extracting(User::getId).isEqualTo(tester.getId());
    }

    @Test
    void findByVerificationOrCurrentEmailTokenHash_returnEmpty_whenNoTokenIssued() {
        final var result = repository.findByVerificationOrCurrentEmailTokenHash(tokenHash);
        assertThat(result).isEmpty();
    }

    @Test
    void findByIdentity_returnUser_whenIdentityIsLinked() {
        repository.save(tester.addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID));

        final var result = repository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID);

        assertThat(result).get().extracting(User::getId).isEqualTo(tester.getId());
    }

    @Test
    void findByIdentity_returnEmpty_whenIdIsLinkedAtOtherProvider() {
        repository.save(tester.addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID));

        final var result = repository.findByIdentity(IdentityProvider.GITHUB, UserTestData.PROVIDER_USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void findWithLockById_returnUser_whenUserExists() {
        final var result = repository.findWithLockById(tester.getId());

        assertThat(result).get().extracting(User::getId).isEqualTo(tester.getId());
    }

    @Test
    void findWithLockById_returnEmpty_whenUserDoesNotExist() {
        final var result = repository.findWithLockById(tester.getId() + 1);

        assertThat(result).isEmpty();
    }

    @Test
    void findById_returnIdentitiesOrderedByProvider_whenUserHasSeveral() {
        repository.save(tester.addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)
                .addIdentity(IdentityProvider.GITHUB, UserTestData.PROVIDER_USER_ID));
        entityManager.flush();
        entityManager.clear();

        final var result = repository.findById(tester.getId()).orElseThrow();

        assertThat(result.getIdentities())
                .extracting(UserIdentity::getProvider)
                .containsExactly(IdentityProvider.GITHUB, IdentityProvider.GOOGLE);
    }

    @Test
    void saveAndFlush_throwDataIntegrityViolation_whenProviderAccountIsLinkedTwice() {
        repository.saveAndFlush(tester.addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID));
        final var merry = UserTestData.createMerry().addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID);

        assertThatThrownBy(() -> repository.saveAndFlush(merry))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlush_throwDataIntegrityViolation_whenUserLinksTwoAccountsAtProvider() {
        tester.addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)
                .addIdentity(IdentityProvider.GOOGLE, UserTestData.OTHER_PROVIDER_USER_ID);

        assertThatThrownBy(() -> repository.saveAndFlush(tester))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlush_storeEmail_whenEmailHasMaximumLength() {
        final var merry = UserTestData.createMerry()
                .setEmail(UserTestData.LONGEST_EMAIL)
                .setPendingEmail(UserTestData.LONGEST_EMAIL);
        repository.saveAndFlush(merry);
        entityManager.clear();

        final var result = repository.findById(merry.getId()).orElseThrow();

        assertThat(result.getEmail()).isEqualTo(UserTestData.LONGEST_EMAIL);
        assertThat(result.getPendingEmail()).isEqualTo(UserTestData.LONGEST_EMAIL);
    }

    @Test
    void deleteByEnabledFalseAndVerificationTokenExpiresBefore_deleteOnlyUnverifiedUsers_whenTokenExpired() {
        final var now = Instant.now();
        final var expired = repository.save(UserTestData.createGimli()
                .setEnabled(false)
                .setVerificationTokenExpires(now.minus(Duration.ofMinutes(1))));
        final var pending = repository.save(UserTestData.createMerry()
                .setEnabled(false)
                .setVerificationTokenExpires(now.plus(Duration.ofMinutes(1))));

        final var deleted = repository.deleteByEnabledFalseAndVerificationTokenExpiresBefore(now);

        assertThat(deleted).isOne();
        assertThat(repository.findAll())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(tester.getId(), pending.getId())
                .doesNotContain(expired.getId());
    }

    @Test
    void existsByEmailIgnoreCase_returnTrue_whenCaseDiffers() {
        final var result = repository.existsByEmailIgnoreCase(tester.getEmail().toUpperCase());
        assertThat(result).isTrue();
    }

    @Test
    void findByUserNameOrEmailAllIgnoreCase_returnUser_whenEmailCaseDiffers() {
        final var input = "Pippin@Shire.ME";
        final var result = repository.findByUserNameOrEmailAllIgnoreCase(input, input);
        assertThat(result).get().extracting(User::getEmail).isEqualTo(tester.getEmail());
    }

    @Test
    void findByUserNameOrEmailAllIgnoreCase_returnUser_whenUserNameCaseDiffers() {
        final var input = tester.getUserName().toUpperCase();
        final var result = repository.findByUserNameOrEmailAllIgnoreCase(input, input);
        assertThat(result).get().extracting(User::getId).isEqualTo(tester.getId());
    }

    @Test
    void existsByUserNameIgnoreCase_returnTrue_whenCaseDiffers() {
        final var result = repository.existsByUserNameIgnoreCase(tester.getUserName().toUpperCase());
        assertThat(result).isTrue();
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
    void saveAndFlush_throwDataIntegrityViolationHandledAsConflict_whenEmailDiffersOnlyInCase() {
        final var merry = UserTestData.createMerry().setEmail(tester.getEmail().toUpperCase());

        final var exception = catchThrowableOfType(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(merry));

        final var problem = new ExceptionController().handleDataIntegrityViolation(exception);
        assertThat(problem.getProperties())
                .containsEntry("details", Map.ofEntries(Map.entry("email", UserTestData.EMAIL_TAKEN)));
    }

    @Test
    void saveAndFlush_storeEmailsLowercased_whenGivenInMixedCase() {
        final var merry = UserTestData.createMerry();
        final var email = merry.getEmail();
        repository.saveAndFlush(merry.setEmail(email.toUpperCase()).setPendingEmail(email.toUpperCase()));
        entityManager.clear();

        final var result = repository.findById(merry.getId()).orElseThrow();

        assertThat(result.getEmail()).isEqualTo(email.toLowerCase());
        assertThat(result.getPendingEmail()).isEqualTo(email.toLowerCase());
    }

}
