package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import pl.prawko.prawko_server.config.JwtConfig;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.exception.InvalidRefreshTokenException;
import pl.prawko.prawko_server.model.RefreshToken;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.RefreshTokenRepository;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.implementation.AccessTokenService;
import pl.prawko.prawko_server.service.implementation.RefreshTokenService;
import pl.prawko.prawko_server.test_data.RefreshTokenTestData;
import pl.prawko.prawko_server.test_data.UserTestData;
import pl.prawko.prawko_server.util.SecureTokens;

import java.time.Instant;
import java.util.Optional;
import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class RefreshTokenServiceTest {

    private final JwtConfig jwtConfig = new JwtConfig();
    private final SecretKey key = jwtConfig.accessTokenKey(null);
    private final JwtDecoder decoder = jwtConfig.jwtDecoder(key, TestUtils.CLOCK);
    private final IAccessTokenService accessTokenService =
            new AccessTokenService(jwtConfig.jwtEncoder(key), TestUtils.ACCESS_TOKEN_VALIDITY, TestUtils.CLOCK);
    private final User tester = UserTestData.createTestUserPippin().setId(TestUtils.PRINCIPAL_ID);

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private UserRepository userRepository;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, userRepository, accessTokenService,
                RefreshTokenTestData.VALIDITY, RefreshTokenTestData.REUSE_GRACE, TestUtils.CLOCK);
    }

    @Nested
    class IssueTokens {

        @Test
        void saveHashedTokenExpiringAfterValidity_whenUserIsAuthenticated() {
            when(userRepository.getReferenceById(tester.getId())).thenReturn(tester);
            final var captor = ArgumentCaptor.forClass(RefreshToken.class);

            final var result = service.issueTokens(TestUtils.createUserPrincipal());

            verify(repository).save(captor.capture());
            final var saved = captor.getValue();
            assertThat(result.refreshToken()).matches("[\\w-]{43}");
            assertThat(saved.getTokenHash())
                    .isEqualTo(SecureTokens.hash(result.refreshToken()))
                    .isNotEqualTo(result.refreshToken());
            assertThat(saved.getUser()).isSameAs(tester);
            assertThat(saved.getExpires()).isEqualTo(TestUtils.CLOCK.instant().plus(RefreshTokenTestData.VALIDITY));
            assertThat(saved.getRevokedAt()).isNull();
            assertThat(saved.getReplacedBy()).isNull();
        }

        @Test
        void returnAccessTokenOfUserWithExpiresIn_whenUserIsAuthenticated() {
            final var principal = TestUtils.createUserPrincipal();

            final var result = service.issueTokens(principal);

            assertThat(decoder.decode(result.accessToken()).getSubject()).isEqualTo(String.valueOf(principal.getId()));
            assertThat(result.expiresIn()).isEqualTo(TestUtils.ACCESS_TOKEN_VALIDITY.toSeconds());
        }

        @Test
        void logUserIdOnly_whenUserIsAuthenticated(final CapturedOutput output) {
            when(userRepository.getReferenceById(tester.getId())).thenReturn(tester);

            service.issueTokens(TestUtils.createUserPrincipal());

            assertThat(output).contains("Refresh token issued for user with id '" + tester.getId() + "'")
                    .doesNotContain(tester.getEmail(), tester.getUserName());
        }

    }

    @Nested
    class Refresh {

        private static final Instant NOW = TestUtils.CLOCK.instant();
        private static final long SUCCESSOR_ID = 2L;

        @Test
        void revokeTokenAndIssueNewPairWithCurrentRole_whenTokenIsValid() {
            tester.setRole(Role.ADMIN);
            final var token = stubToken(NOW.plusSeconds(1));
            when(repository.revokeIfActive(token.getId(), NOW)).thenReturn(1);
            when(userRepository.getReferenceById(tester.getId())).thenReturn(tester);

            final var result = service.refresh(RefreshTokenTestData.TOKEN);

            assertThat(result.refreshToken()).isNotEqualTo(RefreshTokenTestData.TOKEN);
            final var accessToken = decoder.decode(result.accessToken());
            assertThat(accessToken.getSubject()).isEqualTo(String.valueOf(tester.getId()));
            assertThat(accessToken.getClaimAsStringList(JwtConfig.AUTHORITIES_CLAIM))
                    .containsExactly(Role.ADMIN.getAuthority());
            verify(repository).revokeIfActive(token.getId(), NOW);
            assertReplacedByNewToken(token, result.refreshToken());
        }

        @Test
        void revokeSuccessorAndIssueNewPair_whenUsedTokenIsRetriedWithinGracePeriod(final CapturedOutput output) {
            final var token = stubRevokedToken(NOW.minus(RefreshTokenTestData.REUSE_GRACE).plusMillis(1));
            when(repository.revokeIfActive(SUCCESSOR_ID, NOW)).thenReturn(1);
            when(userRepository.getReferenceById(tester.getId())).thenReturn(tester);

            final var result = service.refresh(RefreshTokenTestData.TOKEN);

            assertThat(decoder.decode(result.accessToken()).getSubject()).isEqualTo(String.valueOf(tester.getId()));
            verify(repository).revokeIfActive(SUCCESSOR_ID, NOW);
            assertReplacedByNewToken(token, result.refreshToken());
            assertThat(output).contains("user with id '" + tester.getId() + "' used again within the grace period");
        }

        @Test
        void throwAndRevokeNothing_whenTokenIsUnknown() {
            when(repository.findByTokenHash(SecureTokens.hash(RefreshTokenTestData.TOKEN)))
                    .thenReturn(Optional.empty());

            assertInvalid();

            verifyNoMoreInteractions(repository);
            verifyNoInteractions(userRepository);
        }

        @Test
        void throwAndRevokeNothing_whenTokenIsExpired() {
            stubToken(NOW);

            assertInvalid();

            verifyNoMoreInteractions(repository);
            verifyNoInteractions(userRepository);
        }

        @Test
        void throwAndRevokeAllTokensOfUser_whenTokenIsRevokedWithoutSuccessor(final CapturedOutput output) {
            stubToken(NOW.plusSeconds(1)).setRevokedAt(NOW);
            when(repository.revokeAllByUserId(tester.getId(), NOW)).thenReturn(2);

            assertReused();

            assertThat(output).contains("user with id '" + tester.getId() + "' reused, revoked their 2 active");
        }

        @Test
        void throwAndRevokeAllTokensOfUser_whenUsedTokenIsRetriedAfterGracePeriod() {
            stubRevokedToken(NOW.minus(RefreshTokenTestData.REUSE_GRACE));

            assertReused();

            verify(repository, never()).revokeIfActive(anyLong(), any());
        }

        @Test
        void throwAndRevokeAllTokensOfUser_whenSuccessorIsNoLongerActive() {
            stubRevokedToken(NOW);
            when(repository.revokeIfActive(SUCCESSOR_ID, NOW)).thenReturn(0);

            assertReused();

            verify(repository).revokeIfActive(SUCCESSOR_ID, NOW);
        }

        @Test
        void throwAndRevokeAllTokensOfUser_whenTokenWasRevokedConcurrently() {
            final var token = stubToken(NOW.plusSeconds(1));
            when(repository.revokeIfActive(token.getId(), NOW)).thenReturn(0);

            assertReused();

            verify(repository).revokeIfActive(token.getId(), NOW);
        }

        private RefreshToken stubToken(final Instant expires) {
            final var token = RefreshTokenTestData.createRefreshToken(tester, RefreshTokenTestData.TOKEN, expires)
                    .setId(1L);
            when(repository.findByTokenHash(token.getTokenHash())).thenReturn(Optional.of(token));
            return token;
        }

        private RefreshToken stubRevokedToken(final Instant revokedAt) {
            return stubToken(NOW.plusSeconds(1))
                    .setRevokedAt(revokedAt)
                    .setReplacedBy(new RefreshToken().setId(SUCCESSOR_ID));
        }

        private void assertReplacedByNewToken(final RefreshToken token, final String refreshToken) {
            final var captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getTokenHash()).isEqualTo(SecureTokens.hash(refreshToken));
            verify(repository).setReplacedBy(token.getId(), captor.getValue());
            verify(repository, never()).revokeAllByUserId(anyLong(), any());
        }

        private void assertInvalid() {
            assertThatThrownBy(() -> service.refresh(RefreshTokenTestData.TOKEN))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessage(RefreshTokenTestData.INVALID_REFRESH_TOKEN);
            verify(repository).findByTokenHash(SecureTokens.hash(RefreshTokenTestData.TOKEN));
        }

        private void assertReused() {
            assertInvalid();
            verify(repository).revokeAllByUserId(tester.getId(), NOW);
            verify(repository, never()).save(any());
            verify(repository, never()).setReplacedBy(anyLong(), any());
            verifyNoInteractions(userRepository);
        }

    }

    @Nested
    class Revoke {

        @Test
        void deleteToken_whenTokenExists(final CapturedOutput output) {
            when(repository.deleteByTokenHash(SecureTokens.hash(RefreshTokenTestData.TOKEN))).thenReturn(1);

            service.revoke(RefreshTokenTestData.TOKEN);

            verify(repository).deleteByTokenHash(SecureTokens.hash(RefreshTokenTestData.TOKEN));
            verifyNoMoreInteractions(repository);
            assertThat(output).contains("Refresh token revoked.");
        }

        @Test
        void notThrow_whenTokenIsUnknown(final CapturedOutput output) {
            service.revoke(RefreshTokenTestData.TOKEN);

            verify(repository).deleteByTokenHash(SecureTokens.hash(RefreshTokenTestData.TOKEN));
            assertThat(output).contains("Unknown refresh token revoked");
        }

    }

    @Nested
    class RevokeAll {

        @Test
        void deleteAllTokensOfUser_whenCalled(final CapturedOutput output) {
            when(repository.deleteAllByUserId(tester.getId())).thenReturn(2);

            service.revokeAll(tester.getId());

            verify(repository).deleteAllByUserId(tester.getId());
            verifyNoMoreInteractions(repository);
            assertThat(output).contains("Revoked 2 refresh tokens of user with id '" + tester.getId() + "'.");
        }

    }

    @Nested
    class DeleteExpiredTokens {

        @Test
        void deleteTokensExpiredBeforeNow_whenCalled(final CapturedOutput output) {
            service.deleteExpiredTokens();

            verify(repository).deleteExpiredBefore(TestUtils.CLOCK.instant());
            assertThat(output).doesNotContain("expired refresh tokens");
        }

        @Test
        void logCount_whenTokensDeleted(final CapturedOutput output) {
            when(repository.deleteExpiredBefore(any())).thenReturn(2);

            service.deleteExpiredTokens();

            assertThat(output).contains("Deleted 2 expired refresh tokens.");
        }

    }

}
