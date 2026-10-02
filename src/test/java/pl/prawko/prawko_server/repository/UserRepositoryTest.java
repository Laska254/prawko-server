package pl.prawko.prawko_server.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.test_data.UserTestData;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    private final String wrongUserName = "nonExistingUserName";
    private final String wrongEmail = "nonExistingEmail";
    private final String tokenHash = "a".repeat(63);

    @Autowired
    private UserRepository repository;

    private User tester;

    @BeforeEach
    void setUp() {
        tester = repository.save(UserTestData.createTestUserPippin());
    }

    @Test
    void existsByUserName_returnTrue_whenUserNameExists() {
        final var result = repository.existsByUserName(tester.getUserName());
        assertThat(result).isTrue();
    }

    @Test
    void existsByUserName_returnFalse_whenUserNameDoesNotExist() {
        final var result = repository.existsByUserName(wrongUserName);
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
    void findByUserNameOrEmailIgnoreCase_returnUser_whenFoundByUserName() {
        final var result = repository.findByUserNameOrEmailIgnoreCase(tester.getUserName(), tester.getUserName());
        assertThat(result.get().getUserName()).isEqualTo(tester.getUserName());
    }

    @Test
    void findByUserNameOrEmailIgnoreCase_returnUser_whenFoundByEmail() {
        final var result = repository.findByUserNameOrEmailIgnoreCase(tester.getEmail(), tester.getEmail());
        assertThat(result.get().getEmail()).isEqualTo(tester.getEmail());
    }

    @Test
    void findByUserNameOrEmailIgnoreCase_returnEmpty_whenNotFoundByUserName() {
        final var result = repository.findByUserNameOrEmailIgnoreCase(wrongUserName, wrongUserName);
        assertThat(result).isEmpty();
    }

    @Test
    void findByUserNameOrEmailIgnoreCase_returnEmpty_whenNotFoundByEmail() {
        final var result = repository.findByUserNameOrEmailIgnoreCase(wrongEmail, wrongEmail);
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
    void existsByEmailIgnoreCase_returnTrue_whenCaseDiffers() {
        final var result = repository.existsByEmailIgnoreCase(tester.getEmail().toUpperCase());
        assertThat(result).isTrue();
    }

    @Test
    void findByUserNameOrEmailIgnoreCase_returnUser_whenEmailCaseDiffers() {
        final var input = "Pippin@Shire.ME";
        final var result = repository.findByUserNameOrEmailIgnoreCase(input, input);
        assertThat(result).get().extracting(User::getEmail).isEqualTo(tester.getEmail());
    }

    @Test
    void findByUserNameOrEmailIgnoreCase_returnEmpty_whenUserNameCaseDiffers() {
        final var input = tester.getUserName().toUpperCase();
        final var result = repository.findByUserNameOrEmailIgnoreCase(input, input);
        assertThat(result).isEmpty();
    }

}
