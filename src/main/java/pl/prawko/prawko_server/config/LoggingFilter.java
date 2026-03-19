package pl.prawko.prawko_server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * HTTP request/response logging filter.
 *
 * <p>Intercepts all HTTP requests and responses to log request details (method, URI, client IP,
 * authenticated user) and response details (status code, duration). This provides comprehensive
 * audit trails for debugging and monitoring API usage.
 *
 * <p>The filter is executed once per request, capturing both incoming requests and outgoing responses
 * with precise timing information.
 *
 */
@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    /**
     * Filters HTTP requests and responses with comprehensive logging.
     *
     * <p>Logs the following details:
     * <ul>
     *   <li>Request: method, URI, client IP, authenticated user</li>
     *   <li>Response: HTTP status code, processing duration in milliseconds</li>
     * </ul>
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain for passing request/response to next filter
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain)
            throws ServletException, IOException {
        final var start = System.currentTimeMillis();
        final var method = request.getMethod();
        final var uri = request.getRequestURI();
        final var client = request.getRemoteAddr();
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        final var user = Optional.ofNullable(auth)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElse("anonymous");
        log.info("Request: method={} uri={} client={} user={}", method, uri, client, user);
        try {
            filterChain.doFilter(request, response);
        } finally {
            final var duration = System.currentTimeMillis() - start;
            final var status = response.getStatus();
            log.info("Response: method={} uri={} status={} durationMs={} client={} user={}",
                    method, uri, status, duration, client, user);
        }
    }

}
