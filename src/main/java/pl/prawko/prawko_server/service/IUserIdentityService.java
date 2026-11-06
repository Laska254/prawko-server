package pl.prawko.prawko_server.service;

import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.model.ProviderProfile;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.model.UserIdentity;

/**
 * Service interface for signing in with accounts at external providers and linking them to users as
 * {@link UserIdentity}.
 */
public interface IUserIdentityService {

    /**
     * Finds or creates the {@link User} to sign in with a provider account.
     * <p>
     * The account is looked up by the provider's user ID. If it isn't linked yet, the provider must have verified its
     * email, which then decides the user:
     * <ul>
     *     <li>a verified user with that email gets the account linked;</li>
     *     <li>an unverified user with that email may have been registered by someone else, so its password and pending
     *     verification are cleared and it's enabled before the account is linked;</li>
     *     <li>otherwise a new enabled user without a password is created, with a unique username derived from the
     *     profile.</li>
     * </ul>
     * <p>
     * An existing user is only linked if the provider is authoritative for the email, since otherwise it may have
     * changed hands after the provider verified it.
     *
     * @param profile the profile of the provider account, read from a verified credential
     * @return the user to sign in
     */
    User resolveUser(ProviderProfile profile);

    /**
     * Links a provider account to a signed-in user, who can then sign in with it.
     * <p>
     * Unlike in {@link #resolveUser}, the account's email doesn't matter: the verified credential and the user's session
     * already tie the account to the user, and later sign-ins look it up by the provider's user ID. A notice is emailed
     * to the user's current address.
     *
     * @param userId  the ID of the signed-in user
     * @param profile the profile of the provider account, read from a verified credential
     */
    void linkIdentity(long userId, ProviderProfile profile);

    /**
     * Unlinks a provider account from a signed-in user. Refused if it's the user's only way to sign in. A notice is
     * emailed to the user's current address.
     * <p>
     * If the provider is authoritative for the user's email, {@link #resolveUser} links the account again on its next
     * sign-in, since whoever holds that account controls the mailbox and could reset the password anyway.
     *
     * @param userId   the ID of the signed-in user
     * @param provider the provider whose account to unlink
     */
    void unlinkIdentity(long userId, IdentityProvider provider);

}
