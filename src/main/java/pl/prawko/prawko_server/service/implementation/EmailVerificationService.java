package pl.prawko.prawko_server.service.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DurationFormat;
import org.springframework.format.datetime.standard.DurationFormatterUtils;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import pl.prawko.prawko_server.exception.AlreadyExistsException;
import pl.prawko.prawko_server.exception.InvalidTokenException;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.IEmailVerificationService;
import pl.prawko.prawko_server.util.SecureTokens;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link IEmailVerificationService} sending verification links with {@link MailSender}.
 * <p>
 * Tokens come from {@link SecureTokens}, only their hash is stored.
 */
@Service
public class EmailVerificationService implements IEmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final String INVALID_TOKEN = "Email verification token is invalid or expired.";

    private final UserRepository repository;
    private final MailSender mailSender;
    private final String verificationUrl;
    private final Duration tokenValidity;
    private final Duration cooldown;
    private final String mailFrom;

    public EmailVerificationService(final UserRepository repository,
                                    final MailSender mailSender,
                                    @Value("${email-verification.url}") final String verificationUrl,
                                    @Value("${email-verification.token-validity}") final Duration tokenValidity,
                                    @Value("${email-verification.cooldown}") final Duration cooldown,
                                    @Value("${mail.from}") final String mailFrom) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.verificationUrl = verificationUrl;
        this.tokenValidity = tokenValidity;
        this.cooldown = cooldown;
        this.mailFrom = mailFrom;
    }

    @Override
    public String issueToken(final User user) {
        final var token = SecureTokens.generate();
        user.setVerificationTokenHash(SecureTokens.hash(token))
                .setVerificationTokenExpires(LocalDateTime.now().plus(tokenValidity));
        return token;
    }

    @Override
    public String issueCurrentEmailToken(final User user) {
        final var token = SecureTokens.generate();
        user.setCurrentEmailTokenHash(SecureTokens.hash(token));
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

    /**
     * {@inheritDoc}
     * <p>
     * Both links lead to the same verification page. The emails are sent independently, a failure of either is only
     * logged.
     */
    @Async
    @Override
    public void sendEmailChangeConfirmation(final long userId,
                                            final String currentEmail,
                                            final String newEmail,
                                            final String token,
                                            final String currentEmailToken) {
        send(userId, newEmail, token);
        final var message = new SimpleMailMessage();
        message.setTo(currentEmail);
        message.setSubject("Confirm your email address change");
        message.setText("A change of your account's email address to " + newEmail + " was requested. "
                + "It takes effect only once confirmed with both the link below and the link sent to the new address. "
                + "The link expires in " + printedValidity() + ".\n\n"
                + link(currentEmailToken) + "\n\n"
                + "If you didn't request this, don't open the link. "
                + "Someone else knows your password, so change or reset it, that also cancels the change.");
        deliver(userId, message, "email change confirmation");
    }

    /**
     * {@inheritDoc}
     * <p>
     * The emails are sent independently, a failure of either is only logged.
     */
    @Async
    @Override
    public void sendEmailChangeNotice(final long userId,
                                      final String currentEmail,
                                      final String newEmail,
                                      final String token) {
        send(userId, newEmail, token);
        final var message = new SimpleMailMessage();
        message.setTo(currentEmail);
        message.setSubject("Email address change requested");
        message.setText("An administrator requested a change of your account's email address to " + newEmail
                + ". It takes effect once confirmed with the link sent to the new address.");
        deliver(userId, message, "email change notice");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not transactional, so the token is committed before the email goes out.
     */
    @Async
    @Override
    public void resendVerification(final String email) {
        log.info("Verification email requested.");
        repository.findByEmailIgnoreCase(email)
                .filter(user -> !user.isEnabled())
                .ifPresentOrElse(
                        this::resendVerificationEmail,
                        () -> log.info("No unverified user with the requested email, verification email skipped."));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The pending email is checked for conflicts again, since another user may have taken it in the meantime.
     *
     * @throws InvalidTokenException  if the token is unknown or expired
     * @throws AlreadyExistsException if the pending email already belongs to another user
     */
    @Override
    @Transactional
    public void verify(final String token) {
        final var hash = SecureTokens.hash(token);
        final var user = repository.findByVerificationTokenHashOrCurrentEmailTokenHash(hash, hash)
                .filter(found -> found.getVerificationTokenExpires().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> {
                    log.warn(INVALID_TOKEN);
                    return new InvalidTokenException(INVALID_TOKEN);
                });
        if (hash.equals(user.getCurrentEmailTokenHash())) {
            user.setCurrentEmailTokenHash(null);
        } else {
            user.setVerificationTokenHash(null);
        }
        if (user.getVerificationTokenHash() != null || user.getCurrentEmailTokenHash() != null) {
            repository.save(user);
            log.info("Email change of user with id '{}' confirmed by one address, awaiting the other.", user.getId());
            return;
        }
        final var pendingEmail = user.getPendingEmail();
        if (pendingEmail != null) {
            changeEmail(user, pendingEmail);
        }
        user.setEnabled(true)
                .setVerificationTokenExpires(null);
        repository.save(user);
        log.info("Successfully verified email of user with id '{}'", user.getId());
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${email-verification.cleanup-interval}",
            initialDelayString = "${email-verification.cleanup-interval}")
    public void deleteExpiredUnverifiedUsers() {
        final var deleted = repository.deleteByEnabledFalseAndVerificationTokenExpiresBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Deleted {} unverified users with expired verification tokens.", deleted);
        }
    }

    private void changeEmail(final User user, final String pendingEmail) {
        if (repository.existsByEmailIgnoreCase(pendingEmail)) {
            final var message = "User already exists.";
            log.warn("{} Pending email of user with id '{}' was taken meanwhile.", message, user.getId());
            throw new AlreadyExistsException(message, Map.ofEntries(
                    Map.entry("email", "User with email '" + pendingEmail + "' already exists.")));
        }
        user.setEmail(pendingEmail)
                .setPendingEmail(null)
                .setPasswordResetTokenHash(null)
                .setPasswordResetTokenExpires(null);
        log.info("Email of user with id '{}' changed.", user.getId());
    }

    private void resendVerificationEmail(final User user) {
        if (isCoolingDown(user)) {
            log.info("Verification email for user with id '{}' requested within cooldown, skipped.", user.getId());
            return;
        }
        final var token = issueToken(user);
        repository.save(user);
        send(user.getId(), Objects.requireNonNullElse(user.getPendingEmail(), user.getEmail()), token);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The issue time isn't stored, it's derived from the expiry and the current token validity.
     */
    @Override
    public boolean isCoolingDown(final User user) {
        final var expires = user.getVerificationTokenExpires();
        return expires != null && expires.minus(tokenValidity).plus(cooldown).isAfter(LocalDateTime.now());
    }

    private void send(final long userId, final String email, final String token) {
        final var message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Verify your email address");
        message.setText("To verify your email address, open the link below. It expires in "
                + printedValidity() + ".\n\n"
                + link(token) + "\n\n"
                + "If you didn't request this, ignore this email.");
        deliver(userId, message, "verification email");
    }

    private String link(final String token) {
        return UriComponentsBuilder.fromUriString(verificationUrl)
                .queryParam("token", token)
                .toUriString();
    }

    private String printedValidity() {
        return DurationFormatterUtils.print(tokenValidity, DurationFormat.Style.COMPOSITE);
    }

    private void deliver(final long userId, final SimpleMailMessage message, final String kind) {
        message.setFrom(mailFrom);
        try {
            mailSender.send(message);
            log.info("Sent {} to user with id '{}'", kind, userId);
        } catch (final MailException exception) {
            log.error("Failed to send {} to user with id '{}'", kind, userId, exception);
        }
    }

}
