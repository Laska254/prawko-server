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
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import pl.prawko.prawko_server.exception.AlreadyExistsException;
import pl.prawko.prawko_server.exception.InvalidTokenException;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.implementation.EmailVerificationService;
import pl.prawko.prawko_server.test_data.UserTestData;
import pl.prawko.prawko_server.util.SecureTokens;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class EmailVerificationServiceTest {

    private static final String VERIFICATION_URL = "http://localhost:5173/auth/verify";
    private static final Duration TOKEN_VALIDITY = Duration.ofHours(24);
    private static final Duration COOLDOWN = Duration.ofMinutes(1);
    private static final String MAIL_FROM = "no-reply@prawko.local";

    @Mock
    private UserRepository repository;

    @Mock
    private MailSender mailSender;

    private EmailVerificationService service;

    private final User tester = UserTestData.createUnverifiedPippin().setId(1L);

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(repository, mailSender, VERIFICATION_URL, TOKEN_VALIDITY, COOLDOWN, MAIL_FROM);
    }

    @Nested
    class IssueToken {

        @Test
        void setTokenHashAndExpiry_whenCalled() {
            final var token = service.issueToken(tester);

            assertThat(token).matches("[\\w-]{43}");
            assertThat(tester.getVerificationTokenHash())
                    .isEqualTo(SecureTokens.hash(token))
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
    class IssueCurrentEmailToken {

        @Test
        void setTokenHashAndKeepExpiry_whenCalled() {
            service.issueToken(tester);
            final var expires = tester.getVerificationTokenExpires();

            final var token = service.issueCurrentEmailToken(tester);

            assertThat(token).matches("[\\w-]{43}");
            assertThat(tester.getCurrentEmailTokenHash())
                    .isEqualTo(SecureTokens.hash(token))
                    .isNotEqualTo(tester.getVerificationTokenHash());
            assertThat(tester.getVerificationTokenExpires()).isEqualTo(expires);
            verifyNoInteractions(repository, mailSender);
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
            assertThat(message.getText())
                    .contains(VERIFICATION_URL + "?token=someToken")
                    .contains("expires in 1d.");
            assertThat(UserTestData.extractToken(message)).isEqualTo("someToken");
        }

        @Test
        void logUserIdOnly_whenSent(final CapturedOutput output) {
            service.sendVerificationEmail(tester.getId(), tester.getEmail(), "someToken");

            assertThat(output).contains("Sent verification email to user with id '" + tester.getId() + "'")
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
    class SendEmailChangeConfirmation {

        @Test
        void sendLinksToNewAndCurrentEmail_whenCalled() {
            final var newEmail = UserTestData.createMerry().getEmail();

            service.sendEmailChangeConfirmation(
                    tester.getId(), tester.getEmail(), newEmail, "someToken", "currentToken");

            final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, times(2)).send(captor.capture());
            final var link = captor.getAllValues().getFirst();
            final var confirmation = captor.getAllValues().getLast();
            assertThat(link.getTo()).containsExactly(newEmail);
            assertThat(UserTestData.extractToken(link)).isEqualTo("someToken");
            assertThat(confirmation.getFrom()).isEqualTo(MAIL_FROM);
            assertThat(confirmation.getTo()).containsExactly(tester.getEmail());
            assertThat(confirmation.getText())
                    .startsWith("A change of your account's email address to " + newEmail)
                    .contains(VERIFICATION_URL, "expires in 1d.")
                    .endsWith("If you didn't request this, don't open the link. "
                            + "Someone else knows your password, so change or reset it, that also cancels the change.")
                    .doesNotContain("someToken");
            assertThat(UserTestData.extractToken(confirmation)).isEqualTo("currentToken");
        }

        @Test
        void sendConfirmation_whenSendingLinkFails(final CapturedOutput output) {
            final var newEmail = UserTestData.createMerry().getEmail();
            doThrow(new MailSendException("SMTP down"))
                    .doNothing()
                    .when(mailSender).send(any(SimpleMailMessage.class));

            service.sendEmailChangeConfirmation(
                    tester.getId(), tester.getEmail(), newEmail, "someToken", "currentToken");

            verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
            assertThat(output).contains("Sent email change confirmation to user with id '" + tester.getId() + "'");
        }

    }

    @Nested
    class SendEmailChangeNotice {

        @Test
        void sendLinkToNewEmailAndNoticeToCurrentEmail_whenCalled() {
            final var newEmail = UserTestData.createMerry().getEmail();

            service.sendEmailChangeNotice(tester.getId(), tester.getEmail(), newEmail, "someToken");

            final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, times(2)).send(captor.capture());
            final var notice = captor.getAllValues().getLast();
            assertThat(captor.getAllValues().getFirst().getTo()).containsExactly(newEmail);
            assertThat(notice.getFrom()).isEqualTo(MAIL_FROM);
            assertThat(notice.getTo()).containsExactly(tester.getEmail());
            assertThat(notice.getText())
                    .startsWith("An administrator requested a change of your account's email address to " + newEmail)
                    .doesNotContain(VERIFICATION_URL);
        }

        @Test
        void sendNoticeAndLogUserIdOnly_whenSendingLinkFails(final CapturedOutput output) {
            final var newEmail = UserTestData.createMerry().getEmail();
            doThrow(new MailSendException("SMTP down"))
                    .doNothing()
                    .when(mailSender).send(any(SimpleMailMessage.class));

            service.sendEmailChangeNotice(tester.getId(), tester.getEmail(), newEmail, "someToken");

            verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
            assertThat(output)
                    .contains("Failed to send verification email to user with id '" + tester.getId() + "'")
                    .contains("Sent email change notice to user with id '" + tester.getId() + "'")
                    .doesNotContain(tester.getEmail(), newEmail);
        }

    }

    @Nested
    class IsCoolingDown {

        @Test
        void returnTrue_whenTokenIssuedWithinCooldown() {
            issueTokenIssuedAgo(Duration.ZERO);

            assertThat(service.isCoolingDown(tester)).isTrue();
        }

        @Test
        void returnFalse_whenCooldownHasPassed() {
            issueTokenIssuedAgo(COOLDOWN.plusSeconds(1));

            assertThat(service.isCoolingDown(tester)).isFalse();
        }

        @Test
        void returnFalse_whenNoTokenIssued() {
            assertThat(service.isCoolingDown(tester)).isFalse();
        }

    }

    @Nested
    class ResendVerification {

        @Test
        void storeNewTokenAndSendLink_whenUserIsUnverified() {
            when(repository.findByEmailIgnoreCase(tester.getEmail())).thenReturn(Optional.of(tester));

            service.resendVerification(tester.getEmail());

            final var token = UserTestData.extractToken(captureSentMessage());
            assertThat(tester.getVerificationTokenHash()).isEqualTo(SecureTokens.hash(token));
            verify(repository).save(tester);
        }

        @Test
        void sendLinkToPendingEmail_whenUnverifiedUserHasPendingEmail() {
            final var pendingEmail = UserTestData.createMerry().getEmail();
            tester.setPendingEmail(pendingEmail);
            when(repository.findByEmailIgnoreCase(tester.getEmail())).thenReturn(Optional.of(tester));

            service.resendVerification(tester.getEmail());

            assertThat(captureSentMessage().getTo()).containsExactly(pendingEmail);
        }

        @Test
        void issueNewToken_whenCooldownHasPassed() {
            final var oldHash = issueTokenIssuedAgo(COOLDOWN.plusSeconds(1));
            when(repository.findByEmailIgnoreCase(tester.getEmail())).thenReturn(Optional.of(tester));

            service.resendVerification(tester.getEmail());

            assertThat(tester.getVerificationTokenHash()).isNotEqualTo(oldHash);
            verify(mailSender).send(any(SimpleMailMessage.class));
        }

        @Test
        void keepTokenAndSkipEmail_whenRequestedWithinCooldown() {
            final var hash = issueTokenIssuedAgo(Duration.ZERO);
            when(repository.findByEmailIgnoreCase(tester.getEmail())).thenReturn(Optional.of(tester));

            service.resendVerification(tester.getEmail());

            assertThat(tester.getVerificationTokenHash()).isEqualTo(hash);
            verify(repository, never()).save(any());
            verifyNoInteractions(mailSender);
        }

        @Test
        void doNothing_whenUserIsAlreadyVerified() {
            tester.setEnabled(true);
            when(repository.findByEmailIgnoreCase(tester.getEmail())).thenReturn(Optional.of(tester));

            service.resendVerification(tester.getEmail());

            assertThat(tester.getVerificationTokenHash()).isNull();
            verify(repository, never()).save(any());
            verifyNoInteractions(mailSender);
        }

        @Test
        void doNothingAndNotLogEmail_whenEmailDoesNotExist(final CapturedOutput output) {
            when(repository.findByEmailIgnoreCase(tester.getEmail())).thenReturn(Optional.empty());

            service.resendVerification(tester.getEmail());

            verify(repository, never()).save(any());
            verifyNoInteractions(mailSender);
            assertThat(output).contains("verification email skipped")
                    .doesNotContain(tester.getEmail());
        }

    }

    @Nested
    class Verify {

        @Test
        void enableUserAndClearToken_whenTokenIsValid() {
            final var token = service.issueToken(tester);
            final var hash = tester.getVerificationTokenHash();
            when(repository.findByVerificationTokenHashOrCurrentEmailTokenHash(hash, hash)).thenReturn(Optional.of(tester));

            service.verify(token);

            assertThat(tester.isEnabled()).isTrue();
            assertThat(tester.getVerificationTokenHash()).isNull();
            assertThat(tester.getVerificationTokenExpires()).isNull();
            verify(repository).save(tester);
            verify(repository, never()).existsByEmailIgnoreCase(any());
        }

        @Test
        void changeEmailAndClearResetToken_whenEmailChangeIsPending() {
            final var changing = UserTestData.createPippinChangingEmail()
                    .setPasswordResetTokenHash("pendingTokenHash")
                    .setPasswordResetTokenExpires(LocalDateTime.now().plusMinutes(15));
            final var newEmail = changing.getPendingEmail();
            final var token = service.issueToken(changing);
            final var hash = changing.getVerificationTokenHash();
            when(repository.findByVerificationTokenHashOrCurrentEmailTokenHash(hash, hash)).thenReturn(Optional.of(changing));
            when(repository.existsByEmailIgnoreCase(newEmail)).thenReturn(false);

            service.verify(token);

            assertThat(changing.getEmail()).isEqualTo(newEmail);
            assertThat(changing.getPendingEmail()).isNull();
            assertThat(changing.isEnabled()).isTrue();
            assertThat(changing.getVerificationTokenHash()).isNull();
            assertThat(changing.getPasswordResetTokenHash()).isNull();
            assertThat(changing.getPasswordResetTokenExpires()).isNull();
            verify(repository).save(changing);
        }

        @Test
        void keepEmailAndAwaitCurrentEmail_whenNewEmailConfirmedFirst(final CapturedOutput output) {
            final var changing = UserTestData.createPippinChangingEmail().setId(1L);
            final var email = changing.getEmail();
            final var token = service.issueToken(changing);
            final var currentEmailHash = SecureTokens.hash(service.issueCurrentEmailToken(changing));
            final var hash = changing.getVerificationTokenHash();
            when(repository.findByVerificationTokenHashOrCurrentEmailTokenHash(hash, hash))
                    .thenReturn(Optional.of(changing));

            service.verify(token);

            assertThat(changing.getEmail()).isEqualTo(email);
            assertThat(changing.getPendingEmail()).isNotNull();
            assertThat(changing.getVerificationTokenHash()).isNull();
            assertThat(changing.getCurrentEmailTokenHash()).isEqualTo(currentEmailHash);
            assertThat(changing.getVerificationTokenExpires()).isNotNull();
            verify(repository).save(changing);
            verify(repository, never()).existsByEmailIgnoreCase(any());
            assertThat(output)
                    .contains("Email change of user with id '1' confirmed by one address, awaiting the other.");
        }

        @Test
        void keepEmailAndAwaitNewEmail_whenCurrentEmailConfirmedFirst() {
            final var changing = UserTestData.createPippinChangingEmail();
            final var email = changing.getEmail();
            service.issueToken(changing);
            final var verificationHash = changing.getVerificationTokenHash();
            final var currentEmailToken = service.issueCurrentEmailToken(changing);
            final var hash = changing.getCurrentEmailTokenHash();
            when(repository.findByVerificationTokenHashOrCurrentEmailTokenHash(hash, hash))
                    .thenReturn(Optional.of(changing));

            service.verify(currentEmailToken);

            assertThat(changing.getEmail()).isEqualTo(email);
            assertThat(changing.getCurrentEmailTokenHash()).isNull();
            assertThat(changing.getVerificationTokenHash()).isEqualTo(verificationHash);
            verify(repository).save(changing);
        }

        @Test
        void changeEmail_whenCurrentEmailConfirmsAfterNewEmail() {
            final var changing = UserTestData.createPippinChangingEmail()
                    .setVerificationTokenHash(null);
            final var newEmail = changing.getPendingEmail();
            final var currentEmailToken = service.issueCurrentEmailToken(changing);
            final var hash = changing.getCurrentEmailTokenHash();
            when(repository.findByVerificationTokenHashOrCurrentEmailTokenHash(hash, hash))
                    .thenReturn(Optional.of(changing));
            when(repository.existsByEmailIgnoreCase(newEmail)).thenReturn(false);

            service.verify(currentEmailToken);

            assertThat(changing.getEmail()).isEqualTo(newEmail);
            assertThat(changing.getPendingEmail()).isNull();
            assertThat(changing.getCurrentEmailTokenHash()).isNull();
            assertThat(changing.getVerificationTokenExpires()).isNull();
            verify(repository).save(changing);
        }

        @Test
        void throwAlreadyExistsAndKeepEmail_whenPendingEmailWasTakenMeanwhile(final CapturedOutput output) {
            final var changing = UserTestData.createPippinChangingEmail().setId(1L);
            final var email = changing.getEmail();
            final var newEmail = changing.getPendingEmail();
            final var token = service.issueToken(changing);
            final var hash = changing.getVerificationTokenHash();
            when(repository.findByVerificationTokenHashOrCurrentEmailTokenHash(hash, hash)).thenReturn(Optional.of(changing));
            when(repository.existsByEmailIgnoreCase(newEmail)).thenReturn(true);

            final var exception = catchThrowableOfType(AlreadyExistsException.class, () -> service.verify(token));

            assertThat(exception.getMessage()).isEqualTo(UserTestData.USER_ALREADY_EXISTS);
            assertThat(exception.getDetails())
                    .isEqualTo(Map.ofEntries(Map.entry("email", "User with email '" + newEmail + "' already exists.")));
            assertThat(changing.getEmail()).isEqualTo(email);
            assertThat(changing.getPendingEmail()).isEqualTo(newEmail);
            verify(repository, never()).save(any());
            assertThat(output).contains("user with id '1'")
                    .doesNotContain(newEmail);
        }

        @Test
        void throwInvalidToken_whenTokenIsUnknown() {
            when(repository.findByVerificationTokenHashOrCurrentEmailTokenHash(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.verify("unknownToken"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage(UserTestData.INVALID_VERIFICATION_TOKEN);
            verify(repository, never()).save(any());
        }

        @Test
        void throwInvalidToken_whenTokenIsExpired() {
            final var token = service.issueToken(tester);
            tester.setVerificationTokenExpires(LocalDateTime.now().minusSeconds(1));
            final var hash = tester.getVerificationTokenHash();
            when(repository.findByVerificationTokenHashOrCurrentEmailTokenHash(hash, hash)).thenReturn(Optional.of(tester));

            assertThatThrownBy(() -> service.verify(token))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage(UserTestData.INVALID_VERIFICATION_TOKEN);
            assertThat(tester.isEnabled()).isFalse();
            verify(repository, never()).save(any());
        }

    }

    @Nested
    class DeleteExpiredUnverifiedUsers {

        @Test
        void deleteUnverifiedUsersExpiredBeforeNow_whenCalled() {
            final var captor = ArgumentCaptor.forClass(LocalDateTime.class);

            service.deleteExpiredUnverifiedUsers();

            verify(repository).deleteByEnabledFalseAndVerificationTokenExpiresBefore(captor.capture());
            assertThat(captor.getValue()).isCloseTo(LocalDateTime.now(), within(Duration.ofSeconds(5)));
        }

        @Test
        void logCount_whenUsersDeleted(final CapturedOutput output) {
            when(repository.deleteByEnabledFalseAndVerificationTokenExpiresBefore(any())).thenReturn(2L);

            service.deleteExpiredUnverifiedUsers();

            assertThat(output).contains("Deleted 2 unverified users");
        }

    }

    /**
     * Sets a token on the tester as if it was issued the given time ago.
     */
    private String issueTokenIssuedAgo(final Duration ago) {
        service.issueToken(tester);
        tester.setVerificationTokenExpires(tester.getVerificationTokenExpires().minus(ago));
        return tester.getVerificationTokenHash();
    }

    private SimpleMailMessage captureSentMessage() {
        final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

}
