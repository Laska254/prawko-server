package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.BeforeEach;
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
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.service.implementation.IdentityNotificationService;
import pl.prawko.prawko_server.test_data.UserTestData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class IdentityNotificationServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private MailSender mailSender;

    private IdentityNotificationService service;

    private final String email = UserTestData.createTestUserPippin().getEmail();

    @BeforeEach
    void setUp() {
        service = new IdentityNotificationService(mailSender, TestUtils.MAIL_FROM);
    }

    @Test
    void sendLinkedNotice_sendMailNamingProvider_whenCalled() {
        service.sendLinkedNotice(USER_ID, email, IdentityProvider.GITHUB);

        final var message = sentMessage();
        assertThat(message.getFrom()).isEqualTo(TestUtils.MAIL_FROM);
        assertThat(message.getTo()).containsExactly(email);
        assertThat(message.getSubject()).isEqualTo(UserTestData.identityLinkedSubject(IdentityProvider.GITHUB));
        assertThat(message.getText())
                .contains("A GitHub account was linked")
                .contains("Reset your password");
    }

    @Test
    void sendUnlinkedNotice_sendMailNamingProvider_whenCalled() {
        service.sendUnlinkedNotice(USER_ID, email, IdentityProvider.GOOGLE);

        final var message = sentMessage();
        assertThat(message.getFrom()).isEqualTo(TestUtils.MAIL_FROM);
        assertThat(message.getTo()).containsExactly(email);
        assertThat(message.getSubject()).isEqualTo(UserTestData.identityUnlinkedSubject(IdentityProvider.GOOGLE));
        assertThat(message.getText())
                .contains("The Google account was unlinked")
                .contains("Reset your password");
    }

    @Test
    void sendLinkedNotice_logUserIdOnly_whenSent(final CapturedOutput output) {
        service.sendLinkedNotice(USER_ID, email, IdentityProvider.GOOGLE);

        assertThat(output).contains("Sent identity notice to user with id '" + USER_ID + "'")
                .doesNotContain(email);
    }

    @Test
    void sendLinkedNotice_notThrowAndLogUserIdOnly_whenSendingFails(final CapturedOutput output) {
        doThrow(new MailSendException(TestUtils.MAIL_FAILURE)).when(mailSender).send(any(SimpleMailMessage.class));

        service.sendLinkedNotice(USER_ID, email, IdentityProvider.GOOGLE);

        assertThat(output).contains("Failed to send identity notice to user with id '" + USER_ID + "'")
                .doesNotContain(email);
    }

    @Test
    void sendUnlinkedNotice_notThrowAndLogUserIdOnly_whenSendingFails(final CapturedOutput output) {
        doThrow(new MailSendException(TestUtils.MAIL_FAILURE)).when(mailSender).send(any(SimpleMailMessage.class));

        service.sendUnlinkedNotice(USER_ID, email, IdentityProvider.GOOGLE);

        assertThat(output).contains("Failed to send identity notice to user with id '" + USER_ID + "'")
                .doesNotContain(email);
    }

    private SimpleMailMessage sentMessage() {
        final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

}
