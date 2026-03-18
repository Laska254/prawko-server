package pl.prawko.prawko_server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration class for the application.
 *
 * <p>This configuration enables HTTP Basic Authentication for stateless REST API access
 * with role-based authorization (RBAC). It defines which endpoints are public and which
 * require specific roles (ADMIN, USER) for access.
 *
 * <p>Authorization rules:
 * <ul>
 *     <li>Public: {@code POST /auth} (login), {@code POST /users} (registration)</li>
 *     <li>ADMIN only: {@code POST /questions} (upload), {@code GET /questions} (list all)</li>
 *     <li>USER+ required: {@code GET /questions/**}, {@code POST/GET /exams}</li>
 *     <li>ADMIN only: User management endpoints, delete operations</li>
 *     <li>Public: Swagger UI and OpenAPI docs</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SpringSecurity {

    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * Provides a {@link PasswordEncoder} bean for encoding user passwords.
     * <p>
     * Uses {@link BCryptPasswordEncoder} for password hashing.
     *
     * @return a {@link PasswordEncoder} instance
     */
    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provides the {@link AuthenticationManager} bean used by Spring Security.
     * <p>
     * Allows authentication in the application using globally configured {@link UserDetailsService}
     *
     * @param httpSecurity the {@link HttpSecurity} instance used to build the authentication manager
     * @return an {@link AuthenticationManager} instance
     * @throws Exception if an error occurs while building the authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(final HttpSecurity httpSecurity) throws Exception {
        return httpSecurity.getSharedObject(AuthenticationManagerBuilder.class)
                .build();
    }

    /**
     * Configures global authentication by registering the {@link UserDetailsService} and {@link PasswordEncoder} with the
     * {@link AuthenticationManagerBuilder}.
     *
     * @param auth the {@link AuthenticationManagerBuilder} to configure
     * @throws Exception if an error occurs while setting up the authentication manager
     */
    @Autowired
    public void configureGlobal(final AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }

    /**
     * Configures the application's security filter chain.
     *
     * <p>CSRF protection is disabled because this is a stateless REST API using HTTP Basic Authentication
     * and Bearer tokens, which are inherently protected against CSRF attacks. CSRF protection is only
     * necessary for form-based authentication in stateful sessions.
     *
     * @param http          the {@link HttpSecurity} instance to customize
     * @param loggingFilter the {@link LoggingFilter} for request/response logging
     * @return a fully configured {@link SecurityFilterChain} with defined rules
     * @throws Exception if an error occurs while building the filter chain
     */
    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http,
                                           final LoggingFilter loggingFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterAfter(loggingFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(HttpMethod.POST, "/auth", "/users").permitAll()
                                .requestMatchers(HttpMethod.POST, "/questions").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/questions/**").hasRole("USER")
                                .requestMatchers(HttpMethod.GET, "/questions").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/exams").hasRole("USER")
                                .requestMatchers(HttpMethod.GET, "/exams/**").hasRole("USER")
                                .requestMatchers(HttpMethod.GET, "/users/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PATCH, "/users/**").hasRole("USER")
                                .requestMatchers(HttpMethod.DELETE, "/users/**").hasRole("ADMIN")
                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

}
