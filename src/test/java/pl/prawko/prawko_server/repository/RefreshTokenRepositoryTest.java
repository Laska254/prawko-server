package pl.prawko.prawko_server.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pl.prawko.prawko_server.model.RefreshToken;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.test_data.RefreshTokenTestData;
import pl.prawko.prawko_server.test_data.UserTestData;
import pl.prawko.prawko_server.util.SecureTokens;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RefreshTokenRepositoryTest {

    private final Instant now = Instant.now();

    @Autowired
    private RefreshTokenRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User tester;
    private RefreshToken token;

    @BeforeEach
    void setUp() {
        tester = userRepository.save(UserTestData.createTestUserPippin());
        token = save(tester, RefreshTokenTestData.TOKEN, now.plus(RefreshTokenTestData.VALIDITY));
    }

    @Test
    void findByTokenHash_returnTokenWithUser_whenHashExists() {
        entityManager.clear();

        final var result = repository.findByTokenHash(SecureTokens.hash(RefreshTokenTestData.TOKEN)).orElseThrow();

        assertThat(result).isEqualTo(token);
        entityManager.clear();
        assertThat(result.getUser().getUserName()).isEqualTo(tester.getUserName());
    }

    @Test
    void findByTokenHash_returnEmpty_whenHashDoesNotExist() {
        assertThat(repository.findByTokenHash(SecureTokens.hash("unknownToken"))).isEmpty();
    }

    @Test
    void revokeIfActive_revokeOnlyOnce_whenCalledTwice() {
        assertThat(repository.revokeIfActive(token.getId())).isEqualTo(1);
        assertThat(repository.revokeIfActive(token.getId())).isZero();

        assertThat(reload(token).isRevoked()).isTrue();
    }

    @Test
    void revokeAllByUserId_revokeOnlyActiveTokensOfUser_whenUserHasTokens() {
        final var revoked = save(tester, "revokedToken", now.plus(RefreshTokenTestData.VALIDITY));
        repository.revokeIfActive(revoked.getId());
        final var merry = userRepository.save(UserTestData.createMerry());
        final var othersToken = save(merry, "othersToken", now.plus(RefreshTokenTestData.VALIDITY));

        final var result = repository.revokeAllByUserId(tester.getId());

        assertThat(result).isEqualTo(1);
        assertThat(reload(token).isRevoked()).isTrue();
        assertThat(reload(othersToken).isRevoked()).isFalse();
    }

    @Test
    void deleteByTokenHash_deleteOnlyMatchingToken_whenHashExists() {
        final var other = save(tester, "otherToken", now.plus(RefreshTokenTestData.VALIDITY));

        final var result = repository.deleteByTokenHash(SecureTokens.hash(RefreshTokenTestData.TOKEN));

        assertThat(result).isEqualTo(1);
        assertThat(repository.findAll()).containsExactly(other);
    }

    @Test
    void deleteByTokenHash_deleteNothing_whenHashDoesNotExist() {
        final var result = repository.deleteByTokenHash(SecureTokens.hash("unknownToken"));

        assertThat(result).isZero();
        assertThat(repository.findAll()).containsExactly(token);
    }

    @Test
    void deleteAllByUserId_deleteOnlyTokensOfUser_whenRevokedOrNot() {
        final var revoked = save(tester, "revokedToken", now.plus(RefreshTokenTestData.VALIDITY));
        repository.revokeIfActive(revoked.getId());
        final var merry = userRepository.save(UserTestData.createMerry());
        final var othersToken = save(merry, "othersToken", now.plus(RefreshTokenTestData.VALIDITY));

        final var result = repository.deleteAllByUserId(tester.getId());

        assertThat(result).isEqualTo(2);
        assertThat(repository.findAll()).containsExactly(othersToken);
    }

    @Test
    void deleteExpiredBefore_deleteOnlyExpiredTokens_whenRevokedOrNot() {
        save(tester, "expiredToken", now.minus(Duration.ofMinutes(1)));
        final var expiredRevoked = save(tester, "expiredRevokedToken", now.minus(Duration.ofMinutes(1)));
        repository.revokeIfActive(expiredRevoked.getId());

        final var result = repository.deleteExpiredBefore(now);

        assertThat(result).isEqualTo(2);
        assertThat(repository.findAll()).containsExactly(token);
    }

    private RefreshToken save(final User user, final String value, final Instant expires) {
        return repository.save(RefreshTokenTestData.createRefreshToken(user, value, expires));
    }

    private RefreshToken reload(final RefreshToken refreshToken) {
        entityManager.clear();
        return repository.findById(refreshToken.getId()).orElseThrow();
    }

}
