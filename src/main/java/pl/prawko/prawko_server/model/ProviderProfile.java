package pl.prawko.prawko_server.model;

import org.jspecify.annotations.Nullable;

/**
 * Profile of an account at an {@link IdentityProvider}, read from a credential the provider issued and the server
 * verified.
 *
 * @param provider          the provider of the account
 * @param providerUserId    the provider's stable ID of the account
 * @param email             the account's email, {@code null} if the provider didn't share any
 * @param emailStatus       how far the provider vouches for the email
 * @param suggestedUserName a name to derive the username of a new user from, e.g. the provider's login
 * @param firstName         the first name, {@code null} if unknown
 * @param lastName          the last name, {@code null} if unknown
 */
public record ProviderProfile(

        IdentityProvider provider,
        String providerUserId,
        @Nullable String email,
        EmailStatus emailStatus,
        @Nullable String suggestedUserName,
        @Nullable String firstName,
        @Nullable String lastName

) {
}
