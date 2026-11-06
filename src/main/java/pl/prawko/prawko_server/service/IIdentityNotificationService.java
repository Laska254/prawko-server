package pl.prawko.prawko_server.service;

import pl.prawko.prawko_server.model.IdentityProvider;

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
     * Emails a notice that a provider account was unlinked.
     *
     * @param userId   the ID of the user, for logging
     * @param email    the user's current email address
     * @param provider the provider of the unlinked account
     */
    void sendUnlinkedNotice(long userId, String email, IdentityProvider provider);

}
