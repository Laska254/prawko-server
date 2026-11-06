package pl.prawko.prawko_server.service;

import pl.prawko.prawko_server.model.IdentityProvider;

import java.util.List;

/**
 * Service interface for telling users that a provider account was linked to or unlinked from their account, so they
 * notice if it wasn't them.
 */
public interface IIdentityNotificationService {

    /**
     * Emails a notice that a provider account was linked.
     *
     * @param userId   the ID of the user, for logging
     * @param email    the user's current email address
     * @param provider the provider of the linked account
     */
    void sendLinkedNotice(long userId, String email, IdentityProvider provider);

    /**
     * Emails a notice that signing in linked a provider account to a user who already has other accounts linked, naming
     * those, so the owner of the mailbox can remove one they don't recognize.
     *
     * @param userId         the ID of the user, for logging
     * @param email          the user's current email address
     * @param provider       the provider of the linked account
     * @param otherProviders the providers of the user's other linked accounts
     */
    void sendLinkedBySignInNotice(long userId, String email, IdentityProvider provider,
                                  List<IdentityProvider> otherProviders);

    /**
     * Emails a notice that a provider account was unlinked.
     *
     * @param userId   the ID of the user, for logging
     * @param email    the user's current email address
     * @param provider the provider of the unlinked account
     */
    void sendUnlinkedNotice(long userId, String email, IdentityProvider provider);

}
