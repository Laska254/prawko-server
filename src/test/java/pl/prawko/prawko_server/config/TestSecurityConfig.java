package pl.prawko.prawko_server.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.DynamicPropertyRegistrar;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.IAccessTokenService;
import pl.prawko.prawko_server.test_data.GitHubTestData;
import pl.prawko.prawko_server.test_data.GoogleTestData;

import java.io.IOException;

@TestConfiguration
public class TestSecurityConfig {

    /**
     * Fixed test accounts, authenticated as {@link AuthenticatedUser}. The ID is resolved on every sign-in and
     * {@link TestAuth} token from a persisted user with the same username (0 if none), so ownership checks work against
     * users saved by tests.
     * {@code frodo} hasn't verified his email, so he's disabled.
     */
    @Bean
    @Primary
    public UserDetailsService users(final PasswordEncoder passwordEncoder, final UserRepository userRepository) {
        final var accounts = new InMemoryUserDetailsManager(
                User.withUsername(TestUtils.USER_NAME)
                        .password(passwordEncoder.encode(TestUtils.USER_PASSWORD))
                        .authorities(Role.USER.getAuthority())
                        .build(),
                User.withUsername(TestUtils.ADMIN_NAME)
                        .password(passwordEncoder.encode(TestUtils.ADMIN_PASSWORD))
                        .authorities(Role.ADMIN.getAuthority())
                        .build(),
                User.withUsername(TestUtils.UNVERIFIED_NAME)
                        .password(passwordEncoder.encode(TestUtils.UNVERIFIED_PASSWORD))
                        .authorities(Role.USER.getAuthority())
                        .disabled(true)
                        .build());
        return username -> {
            final var account = accounts.loadUserByUsername(username);
            final var id = userRepository.findByUserNameOrEmailAllIgnoreCase(username, username)
                    .map(pl.prawko.prawko_server.model.User::getId)
                    .orElse(0L);
            return new AuthenticatedUser(
                    id, account.getUsername(), account.getPassword(), account.isEnabled(), account.getAuthorities());
        };
    }

    @Bean
    public GoogleJwksStub googleJwksStub() throws IOException {
        return new GoogleJwksStub(GoogleTestData.jwks());
    }

    /**
     * Enables Google sign-in for the {@link GoogleTestData} client IDs, verified against the {@link GoogleJwksStub}.
     */
    @Bean
    public DynamicPropertyRegistrar googleProperties(final GoogleJwksStub googleJwksStub) {
        return registry -> {
            registry.add("google.jwk-set-uri", googleJwksStub::url);
            registry.add("google.client-ids",
                    () -> String.join(",", GoogleTestData.ANDROID_CLIENT_ID, GoogleTestData.WEB_CLIENT_ID));
        };
    }

    /**
     * Enables GitHub sign-in for the {@link GitHubTestData} client; its requests go to the auto-configured
     * {@link org.springframework.test.web.client.MockRestServiceServer}.
     */
    @Bean
    public DynamicPropertyRegistrar gitHubProperties() {
        return registry -> {
            registry.add("github.client-id", () -> GitHubTestData.CLIENT_ID);
            registry.add("github.client-secret", () -> GitHubTestData.CLIENT_SECRET);
        };
    }

    @Bean
    public TestAuth testAuth(final IAccessTokenService accessTokenService,
                             final UserDetailsService userDetailsService) {
        return new TestAuth(accessTokenService, userDetailsService);
    }

}
