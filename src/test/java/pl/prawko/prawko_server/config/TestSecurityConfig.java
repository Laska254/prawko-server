package pl.prawko.prawko_server.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import pl.prawko.prawko_server.model.AuthenticatedUser;
import pl.prawko.prawko_server.repository.UserRepository;

@TestConfiguration
public class TestSecurityConfig {

    /**
     * Fixed test accounts, authenticated as {@link AuthenticatedUser}. The ID is resolved on every authentication
     * from a persisted user with the same username (0 if none), so ownership checks work against users saved by tests.
     */
    @Bean
    @Primary
    public UserDetailsService users(final PasswordEncoder passwordEncoder, final UserRepository userRepository) {
        final var accounts = new InMemoryUserDetailsManager(
                User.withUsername("pippin")
                        .password(passwordEncoder.encode("lembasy"))
                        .roles("USER")
                        .build(),
                User.withUsername("gimli")
                        .password(passwordEncoder.encode("krasnoludka"))
                        .roles("USER", "ADMIN")
                        .build());
        return username -> {
            final var account = accounts.loadUserByUsername(username);
            final var id = userRepository.findByUserNameOrEmail(username, username)
                    .map(pl.prawko.prawko_server.model.User::getId)
                    .orElse(0L);
            return new AuthenticatedUser(id, account.getUsername(), account.getPassword(), account.getAuthorities());
        };
    }

}
