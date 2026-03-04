package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.service.implementation.RoleService;
import pl.prawko.prawko_server.test_data.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserMapper mapper;

    private static final String[] IGNORED_FIELDS = {"id", "created", "updated", "exams", "password"};

    private final TestDataFactory testDataFactory = new TestDataFactory();
    private final User tester = testDataFactory.createTestUserPippin();

    @Test
    void fromDto_correctlyMapUser() {
        final var dto = testDataFactory.createValidRegisterDto();
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
        final var user = tester.setId(44L);
        final var expected = testDataFactory.createUserDto();

        final var result = mapper.toDto(user);

        assertThat(result).isEqualTo(expected);
    }

}
