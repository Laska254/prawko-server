package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import pl.prawko.prawko_server.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthorizationTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAuthorization userAuthorization;

    private final TestingAuthenticationToken authentication = new TestingAuthenticationToken("pippin", null, "ROLE_USER");

    @Test
    void isSelf_returnTrue_whenUserWithIdHasAuthenticatedUsername() {
        when(userRepository.existsByIdAndUserName(7L, "pippin")).thenReturn(true);

        assertThat(userAuthorization.isSelf(7L, authentication)).isTrue();
    }

    @Test
    void isSelf_returnFalse_whenUserWithIdHasDifferentUsername() {
        when(userRepository.existsByIdAndUserName(8L, "pippin")).thenReturn(false);

        assertThat(userAuthorization.isSelf(8L, authentication)).isFalse();
    }

}
