package pl.prawko.prawko_server.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import pl.prawko.prawko_server.exception.InvalidTokenException;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.implementation.EmailVerificationService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class EmailVerificationServiceTest {

    private static final String VERIFICATION_URL = "http://localhost:5173/auth/verify";
    private static final Duration TOKEN_VALIDITY = Duration.ofHours(24);
    private static final String MAIL_FROM = "no-reply@prawko.local";

    @Mock
    private UserRepository repository;

    @Mock
    private MailSender mailSender;

    private EmailVerificationService service;

    private final User tester = UserTestData.createUnverifiedPippin().setId(1L);

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(repository, mailSender, VERIFICATION_URL, TOKEN_VALIDITY, MAIL_FROM);
    }

    @Nested
    class IssueToken {

        @Test
        void setTokenHashAndExpiry_whenCalled() {
            final var token = service.issueToken(tester);

            assertThat(token).matches("[0-9a-f]{64}");
            assertThat(tester.getVerificationTokenHash())
                    .isEqualTo(DigestUtils.sha256Hex(token))
                    .isNotEqualTo(token);
            assertThat(tester.getVerificationTokenExpires())
                    .isCloseTo(LocalDateTime.now().plus(TOKEN_VALIDITY), within(Duration.ofSeconds(5)));
            verifyNoInteractions(repository, mailSender);
        }

        @Test
        void returnDifferentToken_whenCalledAgain() {
            final var first = service.issueToken(tester);

            assertThat(service.issueToken(tester)).isNotEqualTo(first);
        }

    }

    @Nested
    class SendVerificationEmail {

        @Test
        void sendLinkWithToken_whenCalled() {
            service.sendVerificationEmail(tester.getId(), tester.getEmail(), "someToken");

            final var message = captureSentMessage();
            assertThat(message.getFrom()).isEqualTo(MAIL_FROM);
            assertThat(message.getTo()).containsExactly(tester.getEmail());
            assertThat(message.getText()).contains(VERIFICATION_URL + "?token=someToken");
            assertThat(UserTestData.extractToken(message)).isEqualTo("someToken");
        }

        @Test
        void logUserIdOnly_whenSent(final CapturedOutput output) {
            service.sendVerificationEmail(tester.getId(), tester.getEmail(), "someToken");

            assertThat(output).contains("Verification email sent to user with id '" + tester.getId() + "'")
                    .doesNotContain(tester.getEmail(), "someToken");
        }

        @Test
        void notThrowAndLogUserIdOnly_whenSendingFails(final CapturedOutput output) {
            doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

            service.sendVerificationEmail(tester.getId(), tester.getEmail(), "someToken");

            assertThat(output).contains("Failed to send verification email to user with id '" + tester.getId() + "'")
                    .doesNotContain(tester.getEmail());
        }

    }

    @Nested
    class Verify {

        @Test
        void enableUserAndClearToken_whenTokenIsValid() {
            final var token = service.issueToken(tester);
            when(repository.findByVerificationTokenHash(tester.getVerificationTokenHash())).thenReturn(Optional.of(tester));

            service.verify(token);

            assertThat(tester.isEnabled()).isTrue();
            assertThat(tester.getVerificationTokenHash()).isNull();
            assertThat(tester.getVerificationTokenExpires()).isNull();
            verify(repository).save(tester);
        }

        @Test
        void throwInvalidToken_whenTokenIsUnknown() {
            when(repository.findByVerificationTokenHash(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.verify("unknownToken"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage(UserTestData.INVALID_VERIFICATION_TOKEN);
            verify(repository, never()).save(any());
        }

        @Test
        void throwInvalidToken_whenTokenIsExpired() {
            final var token = service.issueToken(tester);
            tester.setVerificationTokenExpires(LocalDateTime.now().minusSeconds(1));
            when(repository.findByVerificationTokenHash(tester.getVerificationTokenHash())).thenReturn(Optional.of(tester));

            assertThatThrownBy(() -> service.verify(token))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage(UserTestData.INVALID_VERIFICATION_TOKEN);
            assertThat(tester.isEnabled()).isFalse();
            verify(repository, never()).save(any());
        }

    }

    private SimpleMailMessage captureSentMessage() {
        final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

}
