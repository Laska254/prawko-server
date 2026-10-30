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

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
                RefreshTokenTestData.VALIDITY, TestUtils.CLOCK);
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
            assertThat(saved.isRevoked()).isFalse();
        }

        @Test
        void returnAccessTokenOfUserWithExpiresIn_whenUserIsAuthenticated() {
            final var principal = TestUtils.createUserPrincipal();

            final var result = service.issueTokens(principal);

            assertThat(decoder.decode(result.accessToken()).getSubject()).isEqualTo(String.valueOf(principal.getId()));
            assertThat(result.expiresIn()).isEqualTo(TestUtils.ACCESS_TOKEN_VALIDITY.toSeconds());
        }

    }

    @Nested
    class Refresh {

        @Test
        void revokeTokenAndIssueNewPairWithCurrentRole_whenTokenIsValid() {
            tester.setRole(Role.ADMIN);
            final var token = stubToken(TestUtils.CLOCK.instant().plusSeconds(1));
            when(repository.revokeIfActive(token.getId())).thenReturn(1);
            when(userRepository.getReferenceById(tester.getId())).thenReturn(tester);

            final var result = service.refresh(RefreshTokenTestData.TOKEN);

            assertThat(result.refreshToken()).isNotEqualTo(RefreshTokenTestData.TOKEN);
            final var accessToken = decoder.decode(result.accessToken());
            assertThat(accessToken.getSubject()).isEqualTo(String.valueOf(tester.getId()));
            assertThat(accessToken.getClaimAsStringList(JwtConfig.AUTHORITIES_CLAIM))
                    .containsExactly(Role.ADMIN.getAuthority());
            verify(repository).revokeIfActive(token.getId());
            verify(repository).save(any(RefreshToken.class));
        }

        @Test
        void throwAndRevokeNothing_whenTokenIsUnknown() {
            when(repository.findByTokenHash(SecureTokens.hash(RefreshTokenTestData.TOKEN))).thenReturn(Optional.empty());

            assertInvalid();

            verifyNoMoreInteractions(repository);
            verifyNoInteractions(userRepository);
        }

        @Test
        void throwAndRevokeNothing_whenTokenIsExpired() {
            stubToken(TestUtils.CLOCK.instant());

            assertInvalid();

            verifyNoMoreInteractions(repository);
            verifyNoInteractions(userRepository);
        }

        @Test
        void throwAndRevokeAllTokensOfUser_whenTokenIsRevoked(final CapturedOutput output) {
            stubToken(TestUtils.CLOCK.instant().plusSeconds(1)).setRevoked(true);
            when(repository.revokeAllByUserId(tester.getId())).thenReturn(2);

            assertInvalid();

            verify(repository).revokeAllByUserId(tester.getId());
            verifyNoMoreInteractions(repository);
            verifyNoInteractions(userRepository);
            assertThat(output).contains("user with id '" + tester.getId() + "' reused, revoked their 2 active");
        }

        @Test
        void throwAndRevokeAllTokensOfUser_whenTokenWasRevokedConcurrently() {
            final var token = stubToken(TestUtils.CLOCK.instant().plusSeconds(1));
            when(repository.revokeIfActive(token.getId())).thenReturn(0);

            assertInvalid();

            verify(repository).revokeAllByUserId(tester.getId());
            verifyNoMoreInteractions(repository);
            verifyNoInteractions(userRepository);
        }

        private RefreshToken stubToken(final Instant expires) {
            final var token = RefreshTokenTestData.createRefreshToken(tester, RefreshTokenTestData.TOKEN, expires).setId(1L);
            when(repository.findByTokenHash(token.getTokenHash())).thenReturn(Optional.of(token));
            return token;
        }

        private void assertInvalid() {
            assertThatThrownBy(() -> service.refresh(RefreshTokenTestData.TOKEN))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessage(RefreshTokenTestData.INVALID_REFRESH_TOKEN);
            verify(repository).findByTokenHash(SecureTokens.hash(RefreshTokenTestData.TOKEN));
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
