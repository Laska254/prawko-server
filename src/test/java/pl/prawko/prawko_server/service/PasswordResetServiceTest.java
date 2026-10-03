package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.prawko.prawko_server.exception.InvalidTokenException;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.implementation.PasswordResetService;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String RESET_URL = "http://localhost:5173/reset-password";
    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(15);
    private static final String MAIL_FROM = "no-reply@prawko.local";

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailSender mailSender;

    private PasswordResetService service;

    private final User tester = UserTestData.createTestUserPippin().setId(1L);

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(repository, passwordEncoder, mailSender, RESET_URL, TOKEN_VALIDITY, MAIL_FROM);
    }

    @Test
    void requestReset_storeTokenHashAndSendLink_whenEmailExists() {
        when(repository.findByEmailIgnoreCase(tester.getEmail())).thenReturn(Optional.of(tester));

        service.requestReset(tester.getEmail());

        final var message = captureSentMessage();
        final var token = UserTestData.extractResetToken(message);
        assertThat(message.getFrom()).isEqualTo(MAIL_FROM);
        assertThat(message.getTo()).containsExactly(tester.getEmail());
        assertThat(message.getText()).contains(RESET_URL + "?token=" + token);
        assertThat(tester.getPasswordResetTokenHash())
                .hasSize(64)
                .isNotEqualTo(token);
        assertThat(tester.getPasswordResetTokenExpires())
                .isCloseTo(LocalDateTime.now().plus(TOKEN_VALIDITY), within(Duration.ofSeconds(5)));
        verify(repository).save(tester);
    }

    @Test
    void requestReset_issueNewToken_whenRequestedAgain() {
        when(repository.findByEmailIgnoreCase(tester.getEmail())).thenReturn(Optional.of(tester));
        service.requestReset(tester.getEmail());
        final var firstHash = tester.getPasswordResetTokenHash();

        service.requestReset(tester.getEmail());

        assertThat(tester.getPasswordResetTokenHash()).isNotEqualTo(firstHash);
    }

    @Test
    void requestReset_doNothing_whenEmailDoesNotExist() {
        final var email = "nobody@shire.me";
        when(repository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());

        service.requestReset(email);

        verify(repository, never()).save(any());
        verifyNoInteractions(mailSender);
    }

    @Test
    void requestReset_notThrow_whenSendingEmailFails() {
        when(repository.findByEmailIgnoreCase(tester.getEmail())).thenReturn(Optional.of(tester));
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        service.requestReset(tester.getEmail());

        assertThat(tester.getPasswordResetTokenHash()).isNotNull();
        verify(repository).save(tester);
    }

    @Test
    void resetPassword_changePasswordAndClearToken_whenTokenIsValid() {
        final var token = issueToken();
        final var request = UserTestData.createValidResetPasswordRequest(token);
        final var encoded = "encodedPassword";
        when(repository.findByPasswordResetTokenHash(tester.getPasswordResetTokenHash())).thenReturn(Optional.of(tester));
        when(passwordEncoder.encode(request.newPassword())).thenReturn(encoded);

        service.resetPassword(request);

        assertThat(tester.getPassword()).isEqualTo(encoded);
        assertThat(tester.getPasswordResetTokenHash()).isNull();
        assertThat(tester.getPasswordResetTokenExpires()).isNull();
        verify(repository, times(2)).save(tester);
    }

    @Test
    void resetPassword_throwInvalidToken_whenTokenIsUnknown() {
        final var request = UserTestData.createValidResetPasswordRequest("unknownToken");
        when(repository.findByPasswordResetTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage(UserTestData.INVALID_RESET_TOKEN);
        verify(repository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void resetPassword_throwInvalidToken_whenTokenIsExpired() {
        final var token = issueToken();
        tester.setPasswordResetTokenExpires(LocalDateTime.now().minusSeconds(1));
        final var request = UserTestData.createValidResetPasswordRequest(token);
        when(repository.findByPasswordResetTokenHash(tester.getPasswordResetTokenHash())).thenReturn(Optional.of(tester));

        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage(UserTestData.INVALID_RESET_TOKEN);
        verifyNoInteractions(passwordEncoder);
    }

    /**
     * Issues a token through {@link PasswordResetService#requestReset}, so tests use the real hashing.
     */
    private String issueToken() {
        when(repository.findByEmailIgnoreCase(tester.getEmail())).thenReturn(Optional.of(tester));
        service.requestReset(tester.getEmail());
        return UserTestData.extractResetToken(captureSentMessage());
    }

    private SimpleMailMessage captureSentMessage() {
        final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

}
