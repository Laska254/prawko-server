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
    ADMIN

}
