package pl.prawko.prawko_server.service;

import jakarta.persistence.EntityNotFoundException;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.prawko.prawko_server.config.AuthenticatedUser;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.dto.UserUpdateRequest;
import pl.prawko.prawko_server.exception.AlreadyExistsException;
import pl.prawko.prawko_server.exception.InvalidPasswordException;
import pl.prawko.prawko_server.exception.TooManyRequestsException;
import pl.prawko.prawko_server.mapper.UserMapper;
import pl.prawko.prawko_server.model.Role;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.implementation.EmailVerificationService;
import pl.prawko.prawko_server.service.implementation.UserService;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private UserMapper mapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private UserService service;

    private final RegisterDto registerDto = UserTestData.createValidRegisterDto();
    private final User tester = UserTestData.createTestUserPippin();

    @Test
    void register_success_whenUserNotExists() {
        final var user = new User();
        when(repository.existsByUserName(registerDto.userName())).thenReturn(false);
        when(repository.existsByEmailIgnoreCase(registerDto.email())).thenReturn(false);
        when(mapper.fromDto(registerDto)).thenReturn(user);

        service.register(registerDto);

        assertThat(user.getRole()).isEqualTo(Role.USER);
        verify(repository).save(user);
        verify(mapper).fromDto(registerDto);
        verifyNoMoreInteractions(repository, mapper);
    }

    @Test
    void register_issueTokenBeforeSaveAndSendItAfter_whenUserNotExists() {
        final var user = UserTestData.createUnverifiedPippin();
        final var token = "verificationToken";
        when(mapper.fromDto(registerDto)).thenReturn(user);
        when(emailVerificationService.issueToken(user)).thenReturn(token);

        service.register(registerDto);

        final var order = inOrder(emailVerificationService, repository);
        order.verify(emailVerificationService).issueToken(user);
        order.verify(repository).save(user);
        order.verify(emailVerificationService).sendVerificationEmail(user.getId(), user.getEmail(), token);
    }

    @Test
    void register_notSendVerificationEmail_whenUserAlreadyExists() {
        when(repository.existsByUserName(registerDto.userName())).thenReturn(true);

        assertThatThrownBy(() -> service.register(registerDto)).isInstanceOf(AlreadyExistsException.class);

        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void register_throwAlreadyExists_whenExistsByUserName() {
        final var field = "userName";
        when(repository.existsByUserName(registerDto.userName())).thenReturn(true);
        when(repository.existsByEmailIgnoreCase(registerDto.email())).thenReturn(false);

        final ThrowableAssert.ThrowingCallable executable = () -> service.register(registerDto);
        final var exception = catchThrowableOfType(AlreadyExistsException.class, executable);

        assertThat(exception.getMessage()).isEqualTo(UserTestData.USER_ALREADY_EXISTS);
        assertThat(exception.getDetails().get(field))
                .isEqualTo(UserTestData.PIPPIN_CONFLICT_DETAILS.get(field));
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void register_throwAlreadyExists_whenExistsByEmail() {
        final var field = "email";
        when(repository.existsByUserName(registerDto.userName())).thenReturn(false);
        when(repository.existsByEmailIgnoreCase(registerDto.email())).thenReturn(true);

        final ThrowableAssert.ThrowingCallable executable = () -> service.register(registerDto);
        final var exception = catchThrowableOfType(AlreadyExistsException.class, executable);

        assertThat(exception.getMessage()).isEqualTo(UserTestData.USER_ALREADY_EXISTS);
        assertThat(exception.getDetails().get(field))
                .isEqualTo(UserTestData.PIPPIN_CONFLICT_DETAILS.get(field));
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void register_throwAlreadyExists_whenUserNameAndEmailExists() {
        when(repository.existsByUserName(registerDto.userName())).thenReturn(true);
        when(repository.existsByEmailIgnoreCase(registerDto.email())).thenReturn(true);

        final ThrowableAssert.ThrowingCallable executable = () -> service.register(registerDto);
        final var exception = catchThrowableOfType(AlreadyExistsException.class, executable);

        assertThat(exception.getMessage()).isEqualTo(UserTestData.USER_ALREADY_EXISTS);
        assertThat(exception.getDetails()).containsAllEntriesOf(UserTestData.PIPPIN_CONFLICT_DETAILS);
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void register_notLogEmail_whenEmailExists(final CapturedOutput output) {
        when(repository.existsByEmailIgnoreCase(registerDto.email())).thenReturn(true);

        assertThatThrownBy(() -> service.register(registerDto)).isInstanceOf(AlreadyExistsException.class);

        assertThat(output).contains("Conflicting fields: [email]")
                .doesNotContain(registerDto.email());
    }

    @Test
    void getById_returnUser_whenFound() {
        final var given = 44L;
        when(repository.findById(given)).thenReturn(Optional.of(tester));

        final var result = service.getById(given);

        assertThat(result).isEqualTo(tester);
        verify(repository).findById(given);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getUserDtoById_returnUserDto_whenFound() {
        final var given = 44L;
        final var expectedDto = UserTestData.createUserDto(given);
        when(repository.findById(given)).thenReturn(Optional.of(tester));
        when(mapper.toDto(tester)).thenReturn(expectedDto);

        final var result = service.getUserDtoById(given);

        assertThat(result).isEqualTo(expectedDto);
        verify(repository).findById(given);
        verify(mapper).toDto(tester);
        verifyNoMoreInteractions(repository, mapper);
    }

    @Test
    void getUserDtoById_throwException_whenNotFound() {
        final var givenId = 666L;
        when(repository.findById(givenId)).thenReturn(Optional.empty());

        final ThrowableAssert.ThrowingCallable executable = () -> service.getUserDtoById(givenId);
        final var exception = catchThrowableOfType(EntityNotFoundException.class, executable);

        assertThat(exception.getMessage()).isEqualTo(UserTestData.userNotFoundMessage(givenId));
        verify(repository).findById(givenId);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper);
    }

    @Test
    void getAllUsers_returnPageOfUsers_whenFound() {
        final var tester2 = UserTestData.createMerry();
        final var users = List.of(tester, tester2);
        final var pippinDto = UserTestData.createUserDto(4L);
        final var merryDto = new UserDto(45L, tester2.getFirstName(), tester2.getLastName(), tester2.getUserName(), tester2.getEmail(), null, tester2.getRole());
        final var expected = List.of(pippinDto, merryDto);
        final var pageable = PageRequest.of(1, 2);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(users, pageable, 4));
        when(mapper.toDto(tester)).thenReturn(pippinDto);
        when(mapper.toDto(tester2)).thenReturn(merryDto);

        final var result = service.getAllUsers(pageable);

        assertThat(result.getContent()).isEqualTo(expected);
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getPageable()).isEqualTo(pageable);
        verify(repository).findAll(pageable);
        verify(mapper).toDto(tester);
        verify(mapper).toDto(tester2);
        verifyNoMoreInteractions(repository, mapper);
    }

    @Test
    void updateUser_returnUpdatedUser_whenSuccess() {
        final var givenId = 44L;
        final var updateUserRequest = UserTestData.createValidUserUpdateRequest();
        final var updatedUserDto = UserTestData.createUpdatedUserDto(givenId);
        final var user = tester;
        when(repository.findById(givenId)).thenReturn(Optional.of(user));
        when(repository.existsByUserName(updateUserRequest.userName())).thenReturn(false);
        when(repository.existsByEmailIgnoreCase(updateUserRequest.email())).thenReturn(false);
        when(repository.save(user)).thenReturn(user);
        when(mapper.toDto(user)).thenReturn(updatedUserDto);

        final var result = service.updateUser(givenId, updateUserRequest, givenId);

        assertThat(result).isEqualTo(updatedUserDto);
        verify(repository).findById(givenId);
        verify(repository).save(user);
        verify(repository).existsByUserName(updateUserRequest.userName());
        verify(repository).existsByEmailIgnoreCase(updateUserRequest.email());
        verify(mapper).toDto(user);
        verifyNoMoreInteractions(repository, mapper);
    }

    @Test
    void updateUser_keepEmailAndSendLinksToPendingAndCurrentEmail_whenOwnEmailChanged() {
        final var givenId = 44L;
        final var updateUserRequest = UserTestData.createValidUserUpdateRequest();
        final var email = tester.getEmail();
        final var resetHash = "pendingTokenHash";
        final var token = "someToken";
        final var currentEmailToken = "currentToken";
        tester.setId(givenId).setPasswordResetTokenHash(resetHash);
        when(repository.findById(givenId)).thenReturn(Optional.of(tester));
        when(emailVerificationService.issueToken(tester)).thenReturn(token);
        when(emailVerificationService.issueCurrentEmailToken(tester)).thenReturn(currentEmailToken);
        when(repository.save(tester)).thenReturn(tester);

        service.updateUser(givenId, updateUserRequest, givenId);

        assertThat(tester.getEmail()).isEqualTo(email);
        assertThat(tester.getPendingEmail()).isEqualTo(updateUserRequest.email());
        assertThat(tester.getPasswordResetTokenHash()).isEqualTo(resetHash);
        final var order = inOrder(emailVerificationService);
        order.verify(emailVerificationService).issueToken(tester);
        order.verify(emailVerificationService).issueCurrentEmailToken(tester);
        order.verify(emailVerificationService)
                .sendEmailChangeConfirmation(givenId, email, updateUserRequest.email(), token, currentEmailToken);
        verify(repository).save(tester);
    }

    @Test
    void updateUser_requireOnlyPendingEmailLink_whenAdminChangesEmail() {
        final var givenId = 44L;
        final var adminId = 7L;
        final var updateUserRequest = UserTestData.createValidUserUpdateRequest();
        final var changing = UserTestData.createPippinChangingEmail()
                .setId(givenId)
                .setVerificationTokenExpires(LocalDateTime.now().minusMinutes(1))
                .setCurrentEmailTokenHash("previousCurrentTokenHash");
        final var token = "someToken";
        when(repository.findById(givenId)).thenReturn(Optional.of(changing));
        when(emailVerificationService.issueToken(changing)).thenReturn(token);
        when(repository.save(changing)).thenReturn(changing);

        service.updateUser(givenId, updateUserRequest, adminId);

        assertThat(changing.getPendingEmail()).isEqualTo(updateUserRequest.email());
        assertThat(changing.getCurrentEmailTokenHash()).isNull();
        verify(emailVerificationService, never()).issueCurrentEmailToken(any());
        verify(emailVerificationService)
                .sendEmailChangeNotice(givenId, changing.getEmail(), updateUserRequest.email(), token);
        verify(emailVerificationService, never()).sendEmailChangeConfirmation(anyLong(), any(), any(), any(), any());
    }

    @Test
    void updateUser_throwTooManyRequestsAndNotSave_whenEmailChangedWithinCooldown() {
        final var givenId = 44L;
        final var updateUserRequest = UserTestData.createValidUserUpdateRequest();
        when(repository.findById(givenId)).thenReturn(Optional.of(tester));
        when(emailVerificationService.isCoolingDown(tester)).thenReturn(true);

        assertThatThrownBy(() -> service.updateUser(givenId, updateUserRequest, givenId))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessage(UserTestData.VERIFICATION_COOLDOWN);

        assertThat(tester.getPendingEmail()).isNull();
        verify(repository, never()).save(any());
        verify(emailVerificationService, never()).issueToken(any());
        verify(emailVerificationService, never()).sendEmailChangeConfirmation(anyLong(), any(), any(), any(), any());
        verify(emailVerificationService, never()).sendEmailChangeNotice(anyLong(), any(), any(), any());
    }

    @Test
    void updateUser_cancelEmailChangeKeepingExpiry_whenCurrentEmailSent() {
        final var givenId = 44L;
        final var changing = UserTestData.createPippinChangingEmail()
                .setCurrentEmailTokenHash("currentTokenHash");
        final var expires = changing.getVerificationTokenExpires();
        final var currentEmail = changing.getEmail().toUpperCase();
        when(repository.findById(givenId)).thenReturn(Optional.of(changing));
        when(repository.save(changing)).thenReturn(changing);

        service.updateUser(givenId, new UserUpdateRequest(null, null, null, currentEmail), givenId);

        assertThat(changing.getEmail()).isEqualTo(tester.getEmail());
        assertThat(changing.getPendingEmail()).isNull();
        assertThat(changing.getVerificationTokenHash()).isNull();
        assertThat(changing.getCurrentEmailTokenHash()).isNull();
        assertThat(changing.getVerificationTokenExpires()).isEqualTo(expires);
        verify(repository, never()).existsByEmailIgnoreCase(any());
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void updateUser_keepUserUnchanged_whenCurrentEmailSentWithoutPendingChange() {
        final var givenId = 44L;
        final var unverified = UserTestData.createUnverifiedPippin();
        final var token = "someToken";
        unverified.setVerificationTokenHash(token);
        when(repository.findById(givenId)).thenReturn(Optional.of(unverified));
        when(repository.save(unverified)).thenReturn(unverified);

        service.updateUser(givenId, new UserUpdateRequest(null, null, null, unverified.getEmail()), givenId);

        assertThat(unverified.getVerificationTokenHash()).isEqualTo(token);
        verify(repository, never()).existsByEmailIgnoreCase(any());
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void updateUser_notRequestVerification_whenEmailNotProvided() {
        final var givenId = 44L;
        final var updateUserRequest = new UserUpdateRequest("UpdatedFirstName", null, null, null);
        when(repository.findById(givenId)).thenReturn(Optional.of(tester));
        when(repository.save(tester)).thenReturn(tester);

        service.updateUser(givenId, updateUserRequest, givenId);

        assertThat(tester.getPendingEmail()).isNull();
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void updateUser_notLogPersonalData_whenUpdated(final CapturedOutput output) {
        final var givenId = 44L;
        final var updateUserRequest = UserTestData.createValidUserUpdateRequest();
        when(repository.findById(givenId)).thenReturn(Optional.of(tester));
        when(repository.save(tester)).thenReturn(tester);

        service.updateUser(givenId, updateUserRequest, givenId);

        assertThat(output).contains("Successfully updated user with id '" + givenId + "'")
                .doesNotContain(updateUserRequest.firstName(), updateUserRequest.lastName(),
                        updateUserRequest.userName(), updateUserRequest.email());
    }

    @Test
    void updateUser_throwException_whenUserNameAlreadyExists() {
        final var givenId = 44L;
        final var updateUserRequest = UserTestData.createInvalidUserUpdateRequest();
        when(repository.findById(givenId)).thenReturn(Optional.of(tester));
        when(repository.existsByUserName(updateUserRequest.userName())).thenReturn(true);

        final ThrowableAssert.ThrowingCallable executable =
                () -> service.updateUser(givenId, updateUserRequest, givenId);
        final var exception = catchThrowableOfType(AlreadyExistsException.class, executable);

        assertThat(exception.getMessage()).isEqualTo(UserTestData.USER_ALREADY_EXISTS);
        verify(repository).findById(givenId);
        verify(repository).existsByUserName(updateUserRequest.userName());
        verifyNoInteractions(mapper);
    }

    @Test
    void updateUser_throwException_whenEmailAlreadyExists() {
        final var givenId = 44L;
        final var updateUserRequest = UserTestData.createInvalidUserUpdateRequest();
        when(repository.findById(givenId)).thenReturn(Optional.of(tester));
        when(repository.existsByEmailIgnoreCase(updateUserRequest.email())).thenReturn(true);

        final ThrowableAssert.ThrowingCallable executable =
                () -> service.updateUser(givenId, updateUserRequest, givenId);
        final var exception = catchThrowableOfType(AlreadyExistsException.class, executable);

        assertThat(exception.getMessage()).isEqualTo(UserTestData.USER_ALREADY_EXISTS);
        verify(repository).findById(givenId);
        verify(repository).existsByEmailIgnoreCase(updateUserRequest.email());
        verifyNoInteractions(mapper);
    }

    @Test
    void changePassword_success_whenCurrentPasswordMatches() {
        final var givenId = 44L;
        final var request = UserTestData.createValidChangePasswordRequest();
        final var oldEncoded = tester.getPassword();
        final var newEncoded = "encodedNewPassword";
        when(repository.findById(givenId)).thenReturn(Optional.of(tester));
        when(passwordEncoder.matches(request.currentPassword(), oldEncoded)).thenReturn(true);
        when(passwordEncoder.encode(request.newPassword())).thenReturn(newEncoded);

        service.changePassword(givenId, request);

        assertThat(tester.getPassword()).isEqualTo(newEncoded);
        verify(repository).findById(givenId);
        verify(repository).save(tester);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper);
    }

    @Test
    void changePassword_clearResetToken_whenResetTokenPending() {
        final var givenId = 44L;
        final var request = UserTestData.createValidChangePasswordRequest();
        tester.setPasswordResetTokenHash("pendingTokenHash")
                .setPasswordResetTokenExpires(LocalDateTime.now().plusMinutes(30));
        when(repository.findById(givenId)).thenReturn(Optional.of(tester));
        when(passwordEncoder.matches(request.currentPassword(), tester.getPassword())).thenReturn(true);

        service.changePassword(givenId, request);

        assertThat(tester.getPasswordResetTokenHash()).isNull();
        assertThat(tester.getPasswordResetTokenExpires()).isNull();
        verify(repository).save(tester);
    }

    @Test
    void changePassword_cancelEmailChange_whenEmailChangeIsPending() {
        final var givenId = 44L;
        final var changing = UserTestData.createPippinChangingEmail().setCurrentEmailTokenHash("currentTokenHash");
        final var request = UserTestData.createValidChangePasswordRequest();
        when(repository.findById(givenId)).thenReturn(Optional.of(changing));
        when(passwordEncoder.matches(request.currentPassword(), changing.getPassword())).thenReturn(true);

        service.changePassword(givenId, request);

        assertThat(changing.getPendingEmail()).isNull();
        assertThat(changing.getVerificationTokenHash()).isNull();
        assertThat(changing.getCurrentEmailTokenHash()).isNull();
        verify(repository).save(changing);
    }

    @Test
    void changePassword_throwException_whenCurrentPasswordIsIncorrect() {
        final var givenId = 44L;
        final var request = UserTestData.createWrongCurrentPasswordRequest();
        final var oldEncoded = tester.getPassword();
        when(repository.findById(givenId)).thenReturn(Optional.of(tester));
        when(passwordEncoder.matches(request.currentPassword(), oldEncoded)).thenReturn(false);

        final ThrowableAssert.ThrowingCallable executable = () -> service.changePassword(givenId, request);
        final var exception = catchThrowableOfType(InvalidPasswordException.class, executable);

        assertThat(exception.getMessage()).isEqualTo("Current password is incorrect.");
        assertThat(tester.getPassword()).isEqualTo(oldEncoded);
        verify(repository).findById(givenId);
        verify(repository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void changePassword_throwException_whenNewPasswordIsSameAsCurrent() {
        final var givenId = 44L;
        final var request = UserTestData.createSameAsCurrentPasswordRequest();
        final var oldEncoded = tester.getPassword();
        when(repository.findById(givenId)).thenReturn(Optional.of(tester));
        when(passwordEncoder.matches(request.currentPassword(), oldEncoded)).thenReturn(true);

        final ThrowableAssert.ThrowingCallable executable = () -> service.changePassword(givenId, request);
        final var exception = catchThrowableOfType(InvalidPasswordException.class, executable);

        assertThat(exception.getMessage()).isEqualTo("New password must be different from the current one.");
        assertThat(tester.getPassword()).isEqualTo(oldEncoded);
        verify(repository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void changePassword_throwException_whenUserNotFound() {
        final var givenId = 666L;
        final var request = UserTestData.createValidChangePasswordRequest();
        when(repository.findById(givenId)).thenReturn(Optional.empty());

        final ThrowableAssert.ThrowingCallable executable = () -> service.changePassword(givenId, request);
        final var exception = catchThrowableOfType(EntityNotFoundException.class, executable);

        assertThat(exception.getMessage()).isEqualTo(UserTestData.userNotFoundMessage(givenId));
        verify(repository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void deleteUser_success_whenUserExists() {
        final var givenId = 44L;
        when(repository.findById(givenId)).thenReturn(Optional.of(tester));

        service.deleteUser(givenId);

        verify(repository).findById(givenId);
        verify(repository).delete(tester);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper);
    }

    @Test
    void deleteUser_throwException_whenUserNotFound() {
        final var givenId = 666L;
        when(repository.findById(givenId)).thenReturn(Optional.empty());

        final ThrowableAssert.ThrowingCallable executable = () -> service.deleteUser(givenId);
        final var exception = catchThrowableOfType(EntityNotFoundException.class, executable);

        assertThat(exception.getMessage()).isEqualTo(UserTestData.userNotFoundMessage(givenId));
        verify(repository).findById(givenId);
        verify(repository, never()).delete(any());
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper);
    }

    @Test
    void loadUserByUsername_returnUserDetails_whenUserExists() {
        final var user = tester.setId(7L).setRole(Role.USER);
        when(repository.findByUserNameOrEmailIgnoreCase("pippin", "pippin")).thenReturn(Optional.of(user));

        final var result = service.loadUserByUsername("pippin");

        assertThat(result).isInstanceOfSatisfying(AuthenticatedUser.class,
                principal -> assertThat(principal.getId()).isEqualTo(7L));
        assertThat(result.getUsername()).isEqualTo("pippin");
        assertThat(result.getPassword()).isEqualTo(user.getPassword());
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.USER.getAuthority());
    }

    @Test
    void loadUserByUsername_returnDisabledUserDetails_whenEmailIsNotVerified() {
        final var user = UserTestData.createUnverifiedPippin().setId(7L);
        when(repository.findByUserNameOrEmailIgnoreCase(user.getUserName(), user.getUserName())).thenReturn(Optional.of(user));

        final var result = service.loadUserByUsername(user.getUserName());

        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_throwException_whenUserNotExists() {
        when(repository.findByUserNameOrEmailIgnoreCase("nobody", "nobody")).thenReturn(Optional.empty());

        final ThrowableAssert.ThrowingCallable executable = () -> service.loadUserByUsername("nobody");

        assertThatThrownBy(executable)
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage(TestUtils.INVALID_CREDENTIALS);
    }

    @Test
    void loadUserByUsername_logUserIdOnly_whenLoggingInByEmail(final CapturedOutput output) {
        when(repository.findByUserNameOrEmailIgnoreCase(tester.getEmail(), tester.getEmail()))
                .thenReturn(Optional.of(tester.setId(7L).setRole(Role.USER)));

        service.loadUserByUsername(tester.getEmail());

        assertThat(output).contains("User with id '7' loaded successfully.")
                .doesNotContain(tester.getEmail(), tester.getUserName());
    }

    @Test
    void loadUserByUsername_notLogAttemptedLogin_whenUserNotExists(final CapturedOutput output) {
        when(repository.findByUserNameOrEmailIgnoreCase(tester.getEmail(), tester.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername(tester.getEmail()))
                .isInstanceOf(UsernameNotFoundException.class);

        assertThat(output).contains("User not found by username or email.")
                .doesNotContain(tester.getEmail());
    }

}
