package pl.prawko.prawko_server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Authentication event listener for logging security events.
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
        log.info("User {} logged successfully.", success.getAuthentication().getName());
    }

    /**
     * Logs failed authentication events.
     *
     * @param failure the {@link AbstractAuthenticationFailureEvent} containing failure details
     */
    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent failure) {
        log.warn("User {} failed to authenticate. {}", failure.getAuthentication().getName(), failure.getException().getMessage());
    }

}
