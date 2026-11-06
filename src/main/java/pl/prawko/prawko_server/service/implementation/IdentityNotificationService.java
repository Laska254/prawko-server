package pl.prawko.prawko_server.service.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.service.IIdentityNotificationService;

/**
 * Implementation of {@link IIdentityNotificationService} sending notices with {@link MailSender}.
 * <p>
 * Queueing a notice can fail, which callers inside a transaction use to roll back the change. A failure to send it is
 * only logged, since the change has been made by then.
 */
@Service
public class IdentityNotificationService implements IIdentityNotificationService {

    private static final Logger log = LoggerFactory.getLogger(IdentityNotificationService.class);

    private final MailSender mailSender;
    private final String mailFrom;

    public IdentityNotificationService(final MailSender mailSender, @Value("${mail.from}") final String mailFrom) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    @Async
    @Override
    public void sendLinkedNotice(final long userId, final String email, final IdentityProvider provider) {
        final var name = provider.getDisplayName();
        send(userId, email, name + " account linked",
                "A " + name + " account was linked to your account and can now be used to sign in.\n\n"
                        + "If this wasn't you, someone else has access to your account. Reset your password, then "
                        + "unlink the " + name + " account in your account settings.");
    }

    @Async
    @Override
    public void sendUnlinkedNotice(final long userId, final String email, final IdentityProvider provider) {
        final var name = provider.getDisplayName();
        send(userId, email, name + " account unlinked",
                "The " + name + " account was unlinked from your account and can no longer be used to sign in.\n\n"
                        + "If this wasn't you, someone else has access to your account. Reset your password, then "
                        + "link your " + name + " account again in your account settings.");
    }

    private void send(final long userId, final String email, final String subject, final String text) {
        final var message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
            log.info("Sent identity notice to user with id '{}'", userId);
        } catch (final MailException exception) {
            log.error("Failed to send identity notice to user with id '{}'", userId, exception);
        }
    }

}
