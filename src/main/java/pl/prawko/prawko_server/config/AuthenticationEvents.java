package pl.prawko.prawko_server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

/**
 * Authentication event listener for logging security events.
 */
@Component
public class AuthenticationEvents {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationEvents.class);

    /**
     * Logs failed authentication events.
     *
     * <p>The attempted login isn't logged, as it may be an email or a password typed into the wrong field.
     * Failed attempts can still be traced by client IP logged by {@link LoggingFilter}.
     *
     * @param failure the {@link AbstractAuthenticationFailureEvent} containing failure details
     */
    @EventListener
    public void onFailure(final AbstractAuthenticationFailureEvent failure) {
        log.warn("Authentication failed. {}", failure.getException().getMessage());
    }

}
