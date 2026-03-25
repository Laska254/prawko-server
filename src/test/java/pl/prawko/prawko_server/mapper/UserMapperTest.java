package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.Test;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.test_data.UserTestData;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    private static final String[] IGNORED_FIELDS = {"id", "created", "updated", "exams", "password", "roles"};

    private final User tester = UserTestData.createTestUserPippin();

    @Test
    void fromDto_correctlyMapUser() {
        final var dto = UserTestData.createValidRegisterDto();

        final var result = mapper.fromDto(dto);

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
