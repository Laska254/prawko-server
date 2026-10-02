package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.AuthorityUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserTest {

    private final AuthenticatedUser principal =
            new AuthenticatedUser(7L, "pippin", "lembasy", AuthorityUtils.createAuthorityList("ROLE_USER"));

    @Test
    void isSelf_returnTrue_whenIdMatches() {
        assertThat(principal.isSelf(7L)).isTrue();
    }

    @Test
    void isSelf_returnFalse_whenIdDiffers() {
        assertThat(principal.isSelf(8L)).isFalse();
    }

}
