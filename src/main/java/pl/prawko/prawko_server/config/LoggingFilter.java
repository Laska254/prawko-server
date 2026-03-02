package pl.prawko.prawko_server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;


@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(@NonNull final HttpServletRequest request,
                                    @NonNull final HttpServletResponse response,
                                    @NonNull final FilterChain filterChain)
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
