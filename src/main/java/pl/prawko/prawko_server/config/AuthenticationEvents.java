package pl.prawko.prawko_server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Authentication event listener for logging security events.
 *
 * <p>Users are identified by ID only, so no personal data ends up in logs.
 */
@Component
public class AuthenticationEvents {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationEvents.class);

    /**
     * Logs successful authentication events.
     *
     * @param success the {@link AuthenticationSuccessEvent} containing authentication details
     */
    @EventListener
    public void onSuccess(AuthenticationSuccessEvent success) {
        final var userId = success.getAuthentication().getPrincipal() instanceof AuthenticatedUser user
                ? String.valueOf(user.getId())
                : "unknown";
        log.info("User with id '{}' logged successfully.", userId);
    }

    /**
     * Logs failed authentication events.
     *
     * <p>The attempted login isn't logged, as it may be an email or a password typed into the wrong field.
     * Failed attempts can still be traced by client IP logged by {@link LoggingFilter}.
     *
     * @param failure the {@link AbstractAuthenticationFailureEvent} containing failure details
     */
    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent failure) {
        log.warn("Authentication failed. {}", failure.getException().getMessage());
    }

}
