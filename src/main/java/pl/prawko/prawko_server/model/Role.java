package pl.prawko.prawko_server.model;

/**
 * Role of a {@link User}, defining their permissions in the system.
 *
 * <p>Roles are hierarchical: {@link #ADMIN} implies {@link #USER}
 * (see {@link pl.prawko.prawko_server.config.SpringSecurity#roleHierarchy()}).
 */
public enum Role {

    /**
     * Regular user, allowed to take exams and manage their own account.
     */
    USER,

    /**
     * Administrator, allowed to manage questions and all users.
     */
    ADMIN;

    private static final String AUTHORITY_PREFIX = "ROLE_";

    /**
     * Returns the Spring Security authority of this role, e.g. {@code ROLE_ADMIN}.
     *
     * <p>The prefix matches the default one expected by {@code hasRole(...)} expressions.
     *
     * @return the role name prefixed with {@code ROLE_}
     */
    public String getAuthority() {
        return AUTHORITY_PREFIX + name();
    }

}
