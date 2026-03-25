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
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pl.prawko.prawko_server.constants.ApiConstants;

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
     */
    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http,
                                           final LoggingFilter loggingFilter) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterAfter(loggingFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> {
                    authorize
                            .requestMatchers(HttpMethod.POST, ApiConstants.AUTH_BASE_URL).permitAll()
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    configureEndpoint_Users(authorize);
                    configureEndpoint_Questions(authorize);
                    configureEndpoint_Exams(authorize);
                })
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    private void configureEndpoint_Users(AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry authorize) {
        authorize
                .requestMatchers(HttpMethod.POST, ApiConstants.USERS_BASE_URL).permitAll()
                .requestMatchers(HttpMethod.GET, ApiConstants.USERS_BASE_URL_ALL).hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, ApiConstants.USERS_BASE_URL_ALL).hasRole("USER")
                .requestMatchers(HttpMethod.DELETE, ApiConstants.USERS_BASE_URL_ALL).hasRole("ADMIN");
    }

    private void configureEndpoint_Questions(AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry authorize) {
        authorize
                .requestMatchers(HttpMethod.POST, ApiConstants.QUESTIONS_BASE_URL).hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, ApiConstants.QUESTIONS_BASE_URL_ALL).hasRole("USER")
                .requestMatchers(HttpMethod.GET, ApiConstants.QUESTIONS_BASE_URL).hasRole("ADMIN");
    }

    private void configureEndpoint_Exams(AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry authorize) {
        authorize
                .requestMatchers(HttpMethod.POST, ApiConstants.EXAMS_BASE_URL).hasRole("USER")
                .requestMatchers(HttpMethod.GET, ApiConstants.EXAMS_BASE_URL_ALL).hasRole("USER");
    }

}
