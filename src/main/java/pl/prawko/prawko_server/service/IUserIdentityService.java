package pl.prawko.prawko_server.service;

import pl.prawko.prawko_server.model.ProviderProfile;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.model.UserIdentity;

/**
 * Service interface for signing in with accounts at external providers, linked to users as {@link UserIdentity}.
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

}
