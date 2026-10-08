package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.AuthorityUtils;
import pl.prawko.prawko_server.model.Role;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserTest {

    private final AuthenticatedUser principal =
            new AuthenticatedUser(7L, "pippin", "lembasy", AuthorityUtils.createAuthorityList(Role.USER.getAuthority()));

    @Test
    void isSelf_returnTrue_whenIdMatches() {
        assertThat(principal.isSelf(7L)).isTrue();
    }

    @Test
    void isSelf_returnFalse_whenIdDiffers() {
        assertThat(principal.isSelf(8L)).isFalse();
    }

    @Test
    void isAdmin_returnFalse_whenRoleIsUser() {
        assertThat(principal.isAdmin()).isFalse();
    }

    @Test
    void isAdmin_returnTrue_whenRoleIsAdmin() {
        final var admin =
                new AuthenticatedUser(1L, TestUtils.ADMIN_NAME, TestUtils.ADMIN_PASSWORD, AuthorityUtils.createAuthorityList(Role.ADMIN.getAuthority()));

        assertThat(admin.isAdmin()).isTrue();
    }

}
