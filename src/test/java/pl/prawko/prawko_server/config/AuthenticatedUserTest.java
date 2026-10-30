package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserTest {

    private final AuthenticatedUser principal = TestUtils.createUserPrincipal();

    @Test
    void isSelf_returnTrue_whenIdMatches() {
        assertThat(principal.isSelf(principal.getId())).isTrue();
    }

    @Test
    void isSelf_returnFalse_whenIdDiffers() {
        assertThat(principal.isSelf(principal.getId() + 1)).isFalse();
    }

    @Test
    void isAdmin_returnFalse_whenRoleIsUser() {
        assertThat(principal.isAdmin()).isFalse();
    }

    @Test
    void isAdmin_returnTrue_whenRoleIsAdmin() {
        assertThat(TestUtils.createAdminPrincipal().isAdmin()).isTrue();
    }

    @Test
    void idOf_returnId_whenPrincipalIsAuthenticatedUser() {
        final var authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());

        assertThat(AuthenticatedUser.idOf(authentication)).contains(principal.getId());
    }

    @Test
    void idOf_returnEmpty_whenPrincipalIsNotAuthenticatedUser() {
        final var authentication =
                UsernamePasswordAuthenticationToken.unauthenticated(TestUtils.USER_NAME, TestUtils.USER_PASSWORD);

        assertThat(AuthenticatedUser.idOf(authentication)).isEmpty();
    }

    @Test
    void idOf_returnEmpty_whenAuthenticationIsNull() {
        assertThat(AuthenticatedUser.idOf(null)).isEmpty();
    }

}
