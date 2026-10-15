package pl.prawko.prawko_server.service.implementation;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.service.IEmailVerificationService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Implementation of {@link IEmailVerificationService} sending verification links with {@link MailSender}.
 */
@Service
public class EmailVerificationService implements IEmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final int TOKEN_BYTES = 32;
    private static final StringKeyGenerator TOKEN_GENERATOR =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), TOKEN_BYTES);

    private final MailSender mailSender;
    private final String verificationUrl;
    private final Duration tokenValidity;
    private final String mailFrom;

    public EmailVerificationService(final MailSender mailSender,
                                    @Value("${email-verification.url}") final String verificationUrl,
                                    @Value("${email-verification.token-validity}") final Duration tokenValidity,
                                    @Value("${mail.from}") final String mailFrom) {
        this.mailSender = mailSender;
        this.verificationUrl = verificationUrl;
        this.tokenValidity = tokenValidity;
        this.mailFrom = mailFrom;
    }

    @Override
    public String issueToken(final User user) {
        final var token = hash(TOKEN_GENERATOR.generateKey());
        user.setVerificationTokenHash(hash(token))
                .setVerificationTokenExpires(LocalDateTime.now().plus(tokenValidity));
        return token;
    }

    /**
     * {@inheritDoc}
     * <p>
     * A failure to send the email is only logged, the user can request another one.
     */
    @Async
    @Override
    public void sendVerificationEmail(final long userId, final String email, final String token) {
        send(userId, email, token);
    }

    private void send(final long userId, final String email, final String token) {
        final var link = UriComponentsBuilder.fromUriString(verificationUrl)
                .queryParam("token", token)
                .toUriString();
        final var message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("Verify your email address");
        message.setText("To activate your account, open the link below. It expires in "
                + tokenValidity.toHours() + " hours.\n\n"
                + link + "\n\n"
                + "If you didn't create an account, ignore this email.");
        try {
            mailSender.send(message);
            log.info("Verification email sent to user with id '{}'", userId);
        } catch (final MailException exception) {
            log.error("Failed to send verification email to user with id '{}'", userId, exception);
        }
    }

    private static String hash(final String token) {
        return DigestUtils.sha256Hex(token);
    }

}
