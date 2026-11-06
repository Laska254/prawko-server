package pl.prawko.prawko_server.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.authentication.DisabledException;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.exception.AlreadyExistsException;
import pl.prawko.prawko_server.exception.LastSignInMethodException;
import pl.prawko.prawko_server.model.EmailStatus;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.model.ProviderProfile;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.implementation.UserIdentityService;
import pl.prawko.prawko_server.test_data.GoogleTestData;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class UserIdentityServiceTest {

    private static final IdentityProvider PROVIDER = IdentityProvider.GOOGLE;
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserIdentityService service;

    private final ProviderProfile profile = UserTestData.createPippinProfile(PROVIDER);

    private static Stream<Arguments> usersWithEmail() {
        return Stream.of(
                Arguments.of(Named.of("verified", UserTestData.createTestUserPippin().setId(7L))),
                Arguments.of(Named.of("unverified", UserTestData.createUnverifiedPippin().setId(7L)))
        );
    }

    private static Stream<Arguments> suggestedUserNames() {
        final var maxLength = "a".repeat(31);
        return Stream.of(
                Arguments.of(Named.of("valid", "pippin"), "pippin"),
                Arguments.of(Named.of("with '@'", "pippin@shire"), "pippinshire"),
                Arguments.of(Named.of("surrounding whitespace", "  pippin "), "pippin"),
                Arguments.of(Named.of("too long", maxLength + "b"), maxLength),
                Arguments.of(Named.of("too long, cut inside a surrogate pair", "a".repeat(30) + "😀"), "a".repeat(30)),
                Arguments.of(Named.of("too short", "@ab"), "user"),
                Arguments.of(Named.of("blank", "   "), "user"),
                Arguments.of(Named.of("missing", null), "user")
        );
    }

    @Test
    void resolveUser_returnLinkedUserUnchanged_whenIdentityIsLinkedAndEmailNotVerified() {
        final var linked = UserTestData.createGooglePippin().setId(7L);
        when(repository.findByIdentity(PROVIDER, profile.providerUserId())).thenReturn(Optional.of(linked));

        final var result = service.resolveUser(withEmail(null, EmailStatus.UNVERIFIED));

        assertThat(result).isSameAs(linked);
        assertThat(linked.getIdentities()).hasSize(1);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void resolveUser_throwDisabledAndChangeNothing_whenEmailNotVerified() {
        assertThatThrownBy(() -> service.resolveUser(withEmail(profile.email(), EmailStatus.UNVERIFIED)))
                .isInstanceOf(DisabledException.class);

        verify(repository).findByIdentity(PROVIDER, profile.providerUserId());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void resolveUser_throwDisabledAndChangeNothing_whenEmailMissing() {
        assertThatThrownBy(() -> service.resolveUser(withEmail(null, EmailStatus.AUTHORITATIVE)))
                .isInstanceOf(DisabledException.class);

        verify(repository).findByIdentity(PROVIDER, profile.providerUserId());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void resolveUser_linkIdentityKeepingPassword_whenVerifiedUserHasEmail() {
        final var existing = UserTestData.createTestUserPippin().setId(7L);
        final var password = existing.getPassword();
        when(repository.findByEmailIgnoreCase(profile.email())).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenAnswer(returnsFirstArg());

        final var result = service.resolveUser(profile);

        assertThat(result).isSameAs(existing);
        assertThat(result.getPassword()).isEqualTo(password);
        assertLinked(result);
    }

    @Test
    void resolveUser_clearPasswordAndVerificationAndEnable_whenUnverifiedUserHasEmail() {
        final var unverified = UserTestData.createUnverifiedPippin()
                .setId(7L)
                .setVerificationTokenHash("verificationTokenHash")
                .setVerificationTokenExpires(Instant.now().plus(Duration.ofHours(1)));
        when(repository.findByEmailIgnoreCase(profile.email())).thenReturn(Optional.of(unverified));
        when(repository.save(unverified)).thenAnswer(returnsFirstArg());

        final var result = service.resolveUser(profile);

        assertThat(result).isSameAs(unverified);
        assertThat(result.getPassword()).isNull();
        assertThat(result.getVerificationTokenHash()).isNull();
        assertThat(result.getVerificationTokenExpires()).isNull();
        assertThat(result.isEnabled()).isTrue();
        assertLinked(result);
    }

    @Test
    void resolveUser_throwAlreadyExistsAndNotSave_whenUserLinkedToOtherAccountOfProvider() {
        final var existing = UserTestData.createTestUserPippin()
                .setId(7L)
                .addIdentity(PROVIDER, UserTestData.OTHER_PROVIDER_USER_ID);
        when(repository.findByEmailIgnoreCase(profile.email())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.resolveUser(profile))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessage("User is already linked to another Google account.");

        assertThat(existing.getIdentities()).hasSize(1);
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource("usersWithEmail")
    void resolveUser_throwAlreadyExistsAndChangeNothing_whenProviderDoesNotHostEmailOfUser(final User existing) {
        final var password = existing.getPassword();
        final var enabled = existing.isEnabled();
        when(repository.findByEmailIgnoreCase(profile.email())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.resolveUser(withEmail(profile.email(), EmailStatus.VERIFIED)))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessage(GoogleTestData.EMAIL_NOT_HOSTED);

        assertThat(existing.getIdentities()).isEmpty();
        assertThat(existing.getPassword()).isEqualTo(password);
        assertThat(existing.isEnabled()).isEqualTo(enabled);
        verify(repository, never()).save(any());
    }

    @Test
    void resolveUser_createUser_whenProviderDoesNotHostEmailAndNoUserHasIt() {
        stubNewUser();

        final var result = service.resolveUser(withEmail(profile.email(), EmailStatus.VERIFIED));

        assertThat(result.getEmail()).isEqualTo(profile.email());
        assertLinked(result);
    }

    @Test
    void resolveUser_createEnabledUserWithoutPassword_whenNoUserHasEmail() {
        final var pippin = UserTestData.createTestUserPippin();
        stubNewUser();

        final var result = service.resolveUser(profile);

        assertThat(result.getUserName()).isEqualTo(pippin.getUserName());
        assertThat(result.getEmail()).isEqualTo(pippin.getEmail());
        assertThat(result.getFirstName()).isEqualTo(pippin.getFirstName());
        assertThat(result.getLastName()).isEqualTo(pippin.getLastName());
        assertThat(result.getRole()).isEqualTo(Role.USER);
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getPassword()).isNull();
        assertLinked(result);
    }

    @Test
    void resolveUser_cutNamesToMaxLength_whenProfileNamesAreTooLong() {
        final var longName = "a".repeat(32);
        stubNewUser();

        final var result = service.resolveUser(new ProviderProfile(PROVIDER, profile.providerUserId(),
                profile.email(), profile.emailStatus(), profile.suggestedUserName(), longName, longName));

        assertThat(result.getFirstName()).isEqualTo(longName.substring(0, 31));
        assertThat(result.getLastName()).isEqualTo(longName.substring(0, 31));
    }

    @Test
    void resolveUser_keepNamesMissing_whenProfileHasNoNames() {
        stubNewUser();

        final var result = service.resolveUser(new ProviderProfile(PROVIDER, profile.providerUserId(),
                profile.email(), profile.emailStatus(), profile.suggestedUserName(), null, null));

        assertThat(result.getFirstName()).isNull();
        assertThat(result.getLastName()).isNull();
    }

    @ParameterizedTest
    @MethodSource("suggestedUserNames")
    void resolveUser_generateValidUserName_whenSuggestedNameIsGiven(final String suggested, final String expected) {
        stubNewUser();

        final var result = service.resolveUser(withSuggestedUserName(suggested));

        assertThat(result.getUserName()).isEqualTo(expected);
        assertThat(VALIDATOR.validateValue(RegisterDto.class, "userName", result.getUserName())).isEmpty();
    }

    @Test
    void resolveUser_appendLowestFreeSuffix_whenUserNameIsTaken() {
        stubNewUser();
        when(repository.existsByUserNameIgnoreCase("pippin")).thenReturn(true);
        when(repository.existsByUserNameIgnoreCase("pippin1")).thenReturn(true);

        final var result = service.resolveUser(profile);

        assertThat(result.getUserName()).isEqualTo("pippin2");
    }

    @Test
    void resolveUser_cutNameToFitSuffix_whenUserNameOfMaxLengthIsTaken() {
        final var maxLength = "a".repeat(31);
        stubNewUser();
        when(repository.existsByUserNameIgnoreCase(maxLength)).thenReturn(true);

        final var result = service.resolveUser(withSuggestedUserName(maxLength));

        assertThat(result.getUserName()).isEqualTo("a".repeat(30) + "1");
        assertThat(VALIDATOR.validateValue(RegisterDto.class, "userName", result.getUserName())).isEmpty();
    }

    @Test
    void resolveUser_notLogPersonalData_whenCreatingUser(final CapturedOutput output) {
        stubNewUser();

        service.resolveUser(profile);

        assertThat(output).contains("GOOGLE account linked to user with id")
                .doesNotContain(profile.email(), profile.providerUserId(), profile.suggestedUserName());
    }

    @Test
    void linkIdentity_addIdentityKeepingUser_whenEmailDiffersAndIsNotVerified() {
        final var merry = UserTestData.createMerry().setId(7L);
        final var email = merry.getEmail();
        final var password = merry.getPassword();
        when(repository.findById(merry.getId())).thenReturn(Optional.of(merry));

        service.linkIdentity(merry.getId(), withEmail(profile.email(), EmailStatus.UNVERIFIED));

        assertThat(merry.getEmail()).isEqualTo(email);
        assertThat(merry.getPassword()).isEqualTo(password);
        assertLinked(merry);
    }

    @ParameterizedTest
    @ValueSource(strings = {UserTestData.PROVIDER_USER_ID, UserTestData.OTHER_PROVIDER_USER_ID})
    void linkIdentity_throwAlreadyExistsAndNotSave_whenUserHasAccountOfProvider(final String linkedAccount) {
        final var user = UserTestData.createTestUserPippin()
                .setId(7L)
                .addIdentity(PROVIDER, linkedAccount);
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.linkIdentity(user.getId(), profile))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessage(GoogleTestData.ALREADY_LINKED);

        assertThat(user.getIdentities()).hasSize(1);
        verify(repository, never()).save(any());
    }

    @Test
    void linkIdentity_addIdentity_whenUserHasOnlyAccountOfOtherProvider() {
        final var user = UserTestData.createTestUserPippin()
                .setId(7L)
                .addIdentity(IdentityProvider.GITHUB, UserTestData.OTHER_PROVIDER_USER_ID);
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));

        service.linkIdentity(user.getId(), profile);

        assertThat(user.hasIdentity(PROVIDER)).isTrue();
        verify(repository).save(user);
    }

    @Test
    void linkIdentity_throwAlreadyExistsAndNotSave_whenAccountIsLinkedToOtherUser() {
        final var user = UserTestData.createTestUserPippin().setId(7L);
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.findByIdentity(PROVIDER, profile.providerUserId()))
                .thenReturn(Optional.of(UserTestData.createGooglePippin().setId(8L)));

        assertThatThrownBy(() -> service.linkIdentity(user.getId(), profile))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessage(GoogleTestData.LINKED_TO_OTHER_USER);

        assertThat(user.getIdentities()).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void linkIdentity_throwEntityNotFound_whenUserDoesNotExist() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.linkIdentity(7L, profile))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(UserTestData.userNotFoundMessage(7L));
    }

    @Test
    void unlinkIdentity_removeIdentity_whenUserHasPassword() {
        final var user = UserTestData.createTestUserPippin()
                .setId(7L)
                .addIdentity(PROVIDER, UserTestData.PROVIDER_USER_ID);
        when(repository.findWithLockById(user.getId())).thenReturn(Optional.of(user));

        service.unlinkIdentity(user.getId(), PROVIDER);

        assertThat(user.getIdentities()).isEmpty();
        verify(repository).save(user);
    }

    @Test
    void unlinkIdentity_removeOnlyThatIdentity_whenPasswordlessUserHasAnother() {
        final var user = UserTestData.createGooglePippin()
                .setId(7L)
                .addIdentity(IdentityProvider.GITHUB, UserTestData.OTHER_PROVIDER_USER_ID);
        when(repository.findWithLockById(user.getId())).thenReturn(Optional.of(user));

        service.unlinkIdentity(user.getId(), PROVIDER);

        assertThat(user.hasIdentity(PROVIDER)).isFalse();
        assertThat(user.hasIdentity(IdentityProvider.GITHUB)).isTrue();
        verify(repository).save(user);
    }

    @Test
    void unlinkIdentity_throwLastSignInMethodAndNotSave_whenPasswordlessUserHasNoOtherIdentity() {
        final var user = UserTestData.createGooglePippin().setId(7L);
        when(repository.findWithLockById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.unlinkIdentity(user.getId(), PROVIDER))
                .isInstanceOf(LastSignInMethodException.class)
                .hasMessage(UserTestData.LAST_SIGN_IN_METHOD);

        assertThat(user.hasIdentity(PROVIDER)).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void unlinkIdentity_throwEntityNotFound_whenProviderIsNotLinked() {
        final var user = UserTestData.createTestUserPippin()
                .setId(7L)
                .addIdentity(IdentityProvider.GITHUB, UserTestData.PROVIDER_USER_ID);
        when(repository.findWithLockById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.unlinkIdentity(user.getId(), PROVIDER))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(GoogleTestData.NOT_LINKED);

        verify(repository, never()).save(any());
    }

    @Test
    void unlinkIdentity_throwEntityNotFound_whenUserDoesNotExist() {
        when(repository.findWithLockById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unlinkIdentity(7L, PROVIDER))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(UserTestData.userNotFoundMessage(7L));
    }

    private void stubNewUser() {
        when(repository.findByEmailIgnoreCase(profile.email())).thenReturn(Optional.empty());
        when(repository.existsByUserNameIgnoreCase(anyString())).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(returnsFirstArg());
    }

    private void assertLinked(final User user) {
        assertThat(user.getIdentities())
                .singleElement()
                .satisfies(identity -> {
                    assertThat(identity.getProvider()).isEqualTo(PROVIDER);
                    assertThat(identity.getProviderUserId()).isEqualTo(profile.providerUserId());
                    assertThat(identity.getUser()).isSameAs(user);
                });
        verify(repository).save(user);
    }

    private ProviderProfile withEmail(final String email, final EmailStatus status) {
        return new ProviderProfile(PROVIDER, profile.providerUserId(), email, status, profile.suggestedUserName(),
                profile.firstName(), profile.lastName());
    }

    private ProviderProfile withSuggestedUserName(final String suggested) {
        return new ProviderProfile(PROVIDER, profile.providerUserId(), profile.email(), profile.emailStatus(),
                suggested, profile.firstName(), profile.lastName());
    }

}
