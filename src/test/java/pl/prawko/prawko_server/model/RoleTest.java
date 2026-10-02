package pl.prawko.prawko_server.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @ParameterizedTest
    @CsvSource({"USER, ROLE_USER", "ADMIN, ROLE_ADMIN"})
    void getAuthority_returnPrefixedName_whenRoleIsGiven(final Role role, final String expected) {
        assertThat(role.getAuthority()).isEqualTo(expected);
    }

}
