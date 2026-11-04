package pl.prawko.prawko_server.model;

/**
 * External provider a {@link User} can sign in with, linked through a {@link UserIdentity}.
 */
public enum IdentityProvider {

    GOOGLE("Google"),
    GITHUB("GitHub");

    private final String displayName;

    IdentityProvider(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the provider's name as shown to users, e.g. {@code GitHub}.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

}
