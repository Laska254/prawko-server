package pl.prawko.prawko_server.service.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import pl.prawko.prawko_server.dto.ResetPasswordRequest;
import pl.prawko.prawko_server.exception.InvalidTokenException;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.IPasswordResetService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Implementation of {@link IPasswordResetService} sending reset links with {@link MailSender}.
 * <p>
 * Tokens are 256-bit random values. Only their SHA-256 hash is stored, so a database leak doesn't allow resetting
 * passwords. A fast hash is sufficient, since the tokens can't be guessed.
 */
@Service
public class PasswordResetService implements IPasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_BYTES = 32;
    private static final String INVALID_TOKEN = "Password reset token is invalid or expired.";
    private static final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final MailSender mailSender;
    private final String resetUrl;
    private final Duration tokenValidity;
    private final Duration cooldown;
    private final String mailFrom;

    public PasswordResetService(final UserRepository repository,
                                final PasswordEncoder passwordEncoder,
                                final MailSender mailSender,
                                @Value("${password-reset.url}") final String resetUrl,
                                @Value("${password-reset.token-validity}") final Duration tokenValidity,
                                @Value("${password-reset.cooldown}") final Duration cooldown,
                                @Value("${mail.from}") final String mailFrom) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.resetUrl = resetUrl;
        this.tokenValidity = tokenValidity;
        this.cooldown = cooldown;
        this.mailFrom = mailFrom;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Runs asynchronously, as issuing a token and sending the email would otherwise make the response noticeably
     * slower for existing accounts.
     * <p>
     * Not transactional, so the token is committed before the email goes out. A failure to send the email is only
     * logged, as reporting it would reveal that the account exists.
     * <p>
     * The issue time isn't stored, it's derived from the expiry and the current token validity.
     */
    @Async
    @Override
    public void requestReset(final String email) {
        log.info("Password reset requested.");
        repository.findByEmailIgnoreCase(email).ifPresentOrElse(
                this::requestResetEmail,
                () -> log.info("No user with the requested email, password reset skipped."));
    }

    /**
     * {@inheritDoc}
     *
     * @throws InvalidTokenException if the token is unknown or expired
     */
    @Override
    @Transactional
    public void resetPassword(final ResetPasswordRequest request) {
        final var user = repository.findByPasswordResetTokenHash(hash(request.token()))
                .filter(found -> found.getPasswordResetTokenExpires().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> {
                    log.warn(INVALID_TOKEN);
                    return new InvalidTokenException(INVALID_TOKEN);
                });
        user.setPassword(passwordEncoder.encode(request.newPassword()))
                .setPasswordResetTokenHash(null)
                .setPasswordResetTokenExpires(null);
        repository.save(user);
        log.info("Successfully reset password for user with id '{}'", user.getId());
    }

    private boolean isCoolingDown(final User user) {
        final var expires = user.getPasswordResetTokenExpires();
        return expires != null && expires.minus(tokenValidity).plus(cooldown).isAfter(LocalDateTime.now());
    }

    private String issueToken(final User user) {
        final var tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        final var token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        user.setPasswordResetTokenHash(hash(token))
                .setPasswordResetTokenExpires(LocalDateTime.now().plus(tokenValidity));
        repository.save(user);
        log.debug("Password reset token issued for user with id '{}'", user.getId());
        return token;
    }

    private void sendResetEmail(final User user, final String token) {
        final var link = UriComponentsBuilder.fromUriString(resetUrl)
                .queryParam("token", token)
                .toUriString();
        final var message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Password reset");
        message.setText("To reset your password, open the link below. It expires in "
                + tokenValidity.toMinutes() + " minutes.\n\n"
                + link + "\n\n"
                + "If you didn't request a password reset, ignore this email.");
        try {
            mailSender.send(message);
            log.info("Password reset email sent to user with id '{}'", user.getId());
        } catch (final MailException exception) {
            log.error("Failed to send password reset email to user with id '{}'", user.getId(), exception);
        }
    }

    private static String hash(final String token) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private void requestResetEmail(final User user) {
        if (isCoolingDown(user)) {
            log.info("Password reset for user with id '{}' requested within cooldown, skipped.", user.getId());
            return;
        }
        sendResetEmail(user, issueToken(user));
    }

}
