package pl.prawko.prawko_server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.model.Role;

import java.util.List;

/**
 * Spring Security configuration class for the application.
 *
 * <p>This configuration enables HTTP Basic Authentication for stateless REST API access
 * (no HTTP session is created) with role-based authorization (RBAC). It defines which endpoints
 * are public and which require specific roles (ADMIN, USER) for access.
 *
 * <p>Resource ownership (e.g. a user may only modify their own account) is enforced by method security
 * ({@link org.springframework.security.access.prepost.PreAuthorize} and annotations templated on it, like
 * {@link IsSelfOrAdmin}) on controllers.
 *
 * <p>Roles are hierarchical: ADMIN implies USER, so an admin passes every USER rule.
 *
 * <p>Cross-origin requests are allowed from origins matching {@code cors.allowed-origin-patterns}.
 *
 * <p>Authorization rules:
 * <ul>
 *     <li>Public: {@code POST /auth} (login), {@code POST /auth/password/forgot} and {@code POST /auth/password/reset}
 *     (password reset), {@code POST /users} (registration)</li>
 *     <li>ADMIN only: {@code POST /questions} (upload), {@code GET /questions} (list all)</li>
 *     <li>USER+ required: {@code GET /questions/**}, {@code POST/GET /exams}</li>
 *     <li>ADMIN only: User management endpoints, delete operations</li>
 *     <li>Public: Swagger UI and OpenAPI docs</li>
 *     <li>Any other request is denied</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SpringSecurity {

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
     * Defines the role hierarchy, applied to both URL rules and method security.
     *
     * <p>ADMIN implies USER, so an account with only the ADMIN role can access every USER endpoint.
     *
     * @return the {@link RoleHierarchy} where ADMIN implies USER
     */
    @Bean
    public static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withRolePrefix("")
                .role(Role.ADMIN.getAuthority()).implies(Role.USER.getAuthority())
                .build();
    }

    /**
     * Enables placeholders in method security meta-annotations, like {@code {userId}} in {@link IsSelfOrAdmin}.
     *
     * @return default {@link AnnotationTemplateExpressionDefaults}
     */
    @Bean
    public static AnnotationTemplateExpressionDefaults annotationTemplateExpressionDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }

    /**
     * Configures CORS for every endpoint, so a frontend served from another origin can call the API.
     *
     * <p>Spring Security handles preflight ({@code OPTIONS}) requests before authentication, so they don't require
     * credentials. The {@code Location} header is exposed so clients can read the URL of created resources.
     *
     * @param allowedOriginPatterns origin patterns allowed to call the API, e.g. {@code http://localhost:[*]}
     * @return the {@link CorsConfigurationSource} applied by the security filter chain
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origin-patterns}") final List<String> allowedOriginPatterns) {
        final var configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PATCH.name(), HttpMethod.DELETE.name()));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        configuration.setExposedHeaders(List.of(HttpHeaders.LOCATION));
        final var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Provides the {@link AuthenticationManager} bean used by Spring Security.
     * <p>
     * Allows authentication in the application using globally configured {@link UserDetailsService}
     *
     * @param httpSecurity the {@link HttpSecurity} instance used to build the authentication manager
     * @return an {@link AuthenticationManager} instance
     */
    @Bean
    public AuthenticationManager authenticationManager(final HttpSecurity httpSecurity) {
        return httpSecurity.getSharedObject(AuthenticationManagerBuilder.class)
                .build();
    }

    /**
     * Configures global authentication by registering the {@link UserDetailsService} and {@link PasswordEncoder} with the
     * {@link AuthenticationManagerBuilder}.
     *
     * @param auth               the {@link AuthenticationManagerBuilder} to configure
     * @param userDetailsService the {@link UserDetailsService} loading users during authentication
     * @param passwordEncoder    the {@link PasswordEncoder} verifying passwords
     */
    @Autowired
    public void configureGlobal(final AuthenticationManagerBuilder auth,
                                final UserDetailsService userDetailsService,
                                final PasswordEncoder passwordEncoder) {
        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);
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
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterAfter(loggingFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> {
                    authorize
                            .requestMatchers(HttpMethod.POST,
                                    ApiConstants.AUTH_BASE_URL,
                                    ApiConstants.AUTH_BASE_URL + ApiConstants.FORGOT_PASSWORD,
                                    ApiConstants.AUTH_BASE_URL + ApiConstants.RESET_PASSWORD).permitAll()
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    configureEndpoint_Users(authorize);
                    configureEndpoint_Questions(authorize);
                    configureEndpoint_Exams(authorize);
                    authorize.anyRequest().denyAll();
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
                .requestMatchers(HttpMethod.GET, ApiConstants.QUESTIONS_BASE_URL).hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, ApiConstants.QUESTIONS_BASE_URL_ALL).hasRole("USER");
    }

    private void configureEndpoint_Exams(AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry authorize) {
        authorize
                .requestMatchers(HttpMethod.POST, ApiConstants.EXAMS_BASE_URL).hasRole("USER")
                .requestMatchers(HttpMethod.GET, ApiConstants.EXAMS_BASE_URL_ALL).hasRole("USER");
    }

}
