package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.Test;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private static final String[] IGNORED_FIELDS = {"id", "created", "updated", "exams", "password", "role", "enabled"};

    private final UserMapper mapper = new UserMapperImpl();

    private final User tester = UserTestData.createTestUserPippin();

    @Test
    void fromDto_mapUserFields_whenDtoIsValid() {
        final var dto = UserTestData.createValidRegisterDto();

        final var result = mapper.fromDto(dto);

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_FIELDS)
                .isEqualTo(tester);
    }

    @Test
    void fromDto_returnDisabledUser_whenDtoIsValid() {
        final var result = mapper.fromDto(UserTestData.createValidRegisterDto());

        assertThat(result.isEnabled()).isFalse();
        assertThat(result.getVerificationTokenHash()).isNull();
        assertThat(result.getVerificationTokenExpires()).isNull();
    }

    @Test
    void toDto_mapUserFields_whenUserIsPersisted() {
        final var user = tester.setId(1L);
        final var expected = UserTestData.createUserDto(1L);

        final var result = mapper.toDto(user);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toDto_mapPendingEmail_whenEmailChangeIsPending() {
        final var user = UserTestData.createPippinChangingEmail();

        final var result = mapper.toDto(user);

        assertThat(result.email()).isEqualTo(user.getEmail());
        assertThat(result.pendingEmail()).isEqualTo(user.getPendingEmail());
        assertThat(result.awaitingNewEmailConfirmation()).isTrue();
        assertThat(result.awaitingCurrentEmailConfirmation()).isFalse();
    }

    @Test
    void toDto_awaitOnlyCurrentEmailConfirmation_whenNewEmailConfirmedFirst() {
        final var user = UserTestData.createPippinChangingEmail()
                .setVerificationTokenHash(null)
                .setCurrentEmailTokenHash("currentTokenHash");

        final var result = mapper.toDto(user);

        assertThat(result.awaitingNewEmailConfirmation()).isFalse();
        assertThat(result.awaitingCurrentEmailConfirmation()).isTrue();
    }

    @Test
    void toDto_awaitNoConfirmation_whenUnverifiedUserHasNoPendingEmail() {
        final var user = UserTestData.createUnverifiedPippin()
                .setVerificationTokenHash("pendingTokenHash")
                .setVerificationTokenExpires(LocalDateTime.now().plusHours(1));

        final var result = mapper.toDto(user);

        assertThat(result.awaitingNewEmailConfirmation()).isFalse();
        assertThat(result.awaitingCurrentEmailConfirmation()).isFalse();
    }

    @Test
    void toDto_omitPendingEmailAndAwaitNoConfirmation_whenVerificationLinkExpired() {
        final var user = UserTestData.createPippinChangingEmail()
                .setVerificationTokenExpires(LocalDateTime.now().minusMinutes(1))
                .setCurrentEmailTokenHash("currentTokenHash");

        final var result = mapper.toDto(user);

        assertThat(result.pendingEmail()).isNull();
        assertThat(result.awaitingNewEmailConfirmation()).isFalse();
        assertThat(result.awaitingCurrentEmailConfirmation()).isFalse();
    }

}
