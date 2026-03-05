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
 * Spring security configuration class for role-based access using basic authentication.
 * <p>
 * It disables CSRF protection and enforces role-based authorization for all endpoints.
 * <p>
 * Rules:
 * <ul>
 *     <li>Only user with the {@code ADMIN} role can upload file using {@code POST /questions}</li>
 *     <li>Allow public access to {@code POST /users} for registration</li>
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
     * @param http the {@link HttpSecurity} instance to customize
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
                                .requestMatchers(HttpMethod.GET, "/exams/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/users/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PATCH, "users/**").hasRole("USER")
                                .requestMatchers(HttpMethod.DELETE, "/users/**").hasRole("ADMIN")
                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

}
