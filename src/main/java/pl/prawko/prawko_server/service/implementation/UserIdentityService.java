package pl.prawko.prawko_server.service.implementation;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.prawko.prawko_server.exception.AlreadyExistsException;
import pl.prawko.prawko_server.model.ProviderProfile;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.IUserIdentityService;

import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link IUserIdentityService} resolving users from verified provider profiles.
 * <p>
 * Generated usernames follow the registration rules: no {@code '@'}, 3 to 31 characters.
 */
@Service
public class UserIdentityService implements IUserIdentityService {

    private static final Logger log = LoggerFactory.getLogger(UserIdentityService.class);
    private static final int MAX_NAME_LENGTH = 31;
    private static final int MIN_USER_NAME_LENGTH = 3;
    private static final String FALLBACK_USER_NAME = "user";

    private final UserRepository repository;

    public UserIdentityService(final UserRepository repository) {
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     * <p>
     * A linked account signs in even if the provider no longer reports its email as verified, since the email isn't
     * used then.
     * <p>
     * A concurrent sign-in with the same account, or one creating a user with the same username or email, can win the
     * race between the lookups and the save. The resulting {@link DataIntegrityViolationException} is retried once in a
     * new transaction, which then finds the other request's identity or user, or picks another username.
     *
     * @throws DisabledException      if the account isn't linked yet and the provider hasn't verified its email
     * @throws AlreadyExistsException if the user with the email is already linked to another account at the provider,
     *                                or the provider isn't authoritative for the email
     */
    @Override
    @Retryable(includes = DataIntegrityViolationException.class, maxRetries = 1, delay = 100)
    @Transactional
    public User resolveUser(final ProviderProfile profile) {
        final var provider = profile.provider();
        final var linked = repository.findByIdentity(provider, profile.providerUserId());
        if (linked.isPresent()) {
            log.info("User with id '{}' signing in with {}.", linked.get().getId(), provider);
            return linked.get();
        }
        final var email = profile.email();
        if (email == null || !profile.emailVerified()) {
            log.warn("{} account without a verified email rejected.", provider);
            throw new DisabledException("Email address is not verified by " + provider.getDisplayName() + ".");
        }
        final var user = repository.findByEmailIgnoreCase(email)
                .map(existing -> prepareForLinking(existing, profile))
                .orElseGet(() -> createUser(profile, email));
        repository.save(user.addIdentity(provider, profile.providerUserId()));
        log.info("{} account linked to user with id '{}'.", provider, user.getId());
        return user;
    }

    private User prepareForLinking(final User user, final ProviderProfile profile) {
        final var provider = profile.provider();
        if (user.hasIdentity(provider)) {
            final var message = "User is already linked to another " + provider.getDisplayName() + " account.";
            log.warn("{} User id: {}", message, user.getId());
            throw new AlreadyExistsException(message, Map.ofEntries(Map.entry("provider", message)));
        }
        if (!profile.emailAuthoritative()) {
            final var message = "User with this email already exists, and " + provider.getDisplayName()
                    + " doesn't host the email, so the account can't be linked to it.";
            log.warn("{} account not linked to user with id '{}', its email isn't hosted by {}.",
                    provider, user.getId(), provider);
            throw new AlreadyExistsException(message, Map.ofEntries(Map.entry("email", message)));
        }
        if (!user.isEnabled()) {
            user.setPassword(null)
                    .setVerificationTokenHash(null)
                    .setVerificationTokenExpires(null)
                    .setEnabled(true);
            log.info("Unverified user with id '{}' verified by {}, its password cleared.", user.getId(), provider);
        }
        return user;
    }

    private User createUser(final ProviderProfile profile, final String email) {
        log.info("Creating user for a {} account.", profile.provider());
        return new User()
                .setUserName(generateUserName(profile.suggestedUserName()))
                .setEmail(email)
                .setFirstName(cut(profile.firstName(), MAX_NAME_LENGTH))
                .setLastName(cut(profile.lastName(), MAX_NAME_LENGTH))
                .setRole(Role.USER)
                .setEnabled(true);
    }

    /**
     * Derives a username from the suggested name: without {@code '@'} and surrounding whitespace, cut to the maximum
     * length, falling back to {@code "user"} if too short. A taken name gets the lowest free numeric
     * suffix, cutting the name further to fit.
     */
    private String generateUserName(@Nullable final String suggested) {
        final var base = Optional.ofNullable(suggested)
                .map(name -> name.replace("@", "").strip())
                .filter(name -> name.length() >= MIN_USER_NAME_LENGTH)
                .orElse(FALLBACK_USER_NAME);
        var userName = cut(base, MAX_NAME_LENGTH);
        for (var suffix = 1; repository.existsByUserNameIgnoreCase(userName); suffix++) {
            final var digits = String.valueOf(suffix);
            userName = cut(base, MAX_NAME_LENGTH - digits.length()) + digits;
        }
        return userName;
    }

    /**
     * Cuts the text to the maximum length without splitting a surrogate pair.
     */
    private static @Nullable String cut(@Nullable final String text, final int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        if (Character.isHighSurrogate(text.charAt(maxLength - 1))) {
            return text.substring(0, maxLength - 1);
        }
        return text.substring(0, maxLength);
    }

}
