package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.service.implementation.RoleService;
import pl.prawko.prawko_server.test_data.UserTestData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UserMapperImpl.class)
class UserMapperTest {

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper mapper;

    private static final String[] IGNORED_FIELDS = {"id", "created", "updated", "exams", "password"};

    private final User tester = UserTestData.createTestUserPippin();

    @Test
    void fromDto_correctlyMapUser() {
        final var dto = UserTestData.createValidRegisterDto();
        final var role = new Role().setName("USER");
        when(roleService.getByName(role.getName())).thenReturn(role);
        when(passwordEncoder.encode(dto.password())).thenReturn("hashed");

        final var result = mapper.fromDto(dto);

        assertThat(result.getPassword()).isEqualTo("hashed");
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(tester);
    }

    @Test
    void toDto_correctlyMapUser() {
        final var user = tester.setId(1L);
        final var expected = UserTestData.createUserDto();

        final var result = mapper.toDto(user);

        assertThat(result).isEqualTo(expected);
    }

}
