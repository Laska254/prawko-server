package pl.prawko.prawko_server.service;

import jakarta.persistence.EntityNotFoundException;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.exception.AlreadyExistsException;
import pl.prawko.prawko_server.mapper.UserMapper;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.implementation.UserService;
import pl.prawko.prawko_server.test_data.TestDataFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Map<String, String> EXPECTED = Map.of(
            "userName", "User with username 'pippin' already exists.",
            "email", "User with email 'pippin@shire.me' already exists."
    );

    private static final String ERROR_MESSAGE = "User already exists.";

    @Mock
    private UserRepository repository;

    @Mock
    private UserMapper mapper;

    @InjectMocks
    private UserService service;

    private final TestDataFactory testDataFactory = new TestDataFactory();
    private final RegisterDto registerDto = testDataFactory.createValidRegisterDto();
    private final User tester = testDataFactory.createTestUserPippin();

    @Test
    void register_success_whenUserNotExists() {
        when(repository.existsByUserName(registerDto.userName())).thenReturn(false);
        when(repository.existsByEmail(registerDto.email())).thenReturn(false);
        final var user = new User();
        when(mapper.fromDto(registerDto)).thenReturn(user);

        service.register(registerDto);

        verify(repository).save(user);
    }

    @Test
    void register_throwAlreadyExists_whenExistsByUserName() {
        final var field = "userName";
        when(repository.existsByUserName(registerDto.userName())).thenReturn(true);
        when(repository.existsByEmail(registerDto.email())).thenReturn(false);

        final ThrowableAssert.ThrowingCallable executable = () -> service.register(registerDto);
        final var exception = catchThrowableOfType(AlreadyExistsException.class, executable);

        assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
        assertThat(exception.getDetails().get(field))
                .isEqualTo(EXPECTED.get(field));
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void register_throwAlreadyExists_whenExistsByEmail() {
        final var field = "email";
        when(repository.existsByUserName(registerDto.userName())).thenReturn(false);
        when(repository.existsByEmail(registerDto.email())).thenReturn(true);

        final ThrowableAssert.ThrowingCallable executable = () -> service.register(registerDto);
        final var exception = catchThrowableOfType(AlreadyExistsException.class, executable);

        assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
        assertThat(exception.getDetails().get(field))
                .isEqualTo(EXPECTED.get(field));
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void register_throwAlreadyExists_whenUserNameAndEmailExists() {
        when(repository.existsByUserName(registerDto.userName())).thenReturn(true);
        when(repository.existsByEmail(registerDto.email())).thenReturn(true);

        final ThrowableAssert.ThrowingCallable executable = () -> service.register(registerDto);
        final var exception = catchThrowableOfType(AlreadyExistsException.class, executable);

        assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
        assertThat(exception.getDetails()).containsAllEntriesOf(EXPECTED);
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getByUserNameOrEmail_returnUser_whenFoundByUserName() {
        final var userNameOrEmail = "pippin";
        when(repository.findByUserNameOrEmail(userNameOrEmail)).thenReturn(Optional.of(tester));

        final var result = service.getByUserNameOrEmail(userNameOrEmail);

        assertThat(result).isEqualTo(tester);
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getByUserNameOrEmail_returnUser_whenFoundByEmail() {
        final var userNameOrEmail = "pippin@shire.me";
        when(repository.findByUserNameOrEmail(userNameOrEmail)).thenReturn(Optional.of(tester));

        final var result = service.getByUserNameOrEmail(userNameOrEmail);

        assertThat(result).isEqualTo(tester);
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getByUserNameOrEmail_throwException_whenNotFound() {
        final var userNameOrEmail = "wrongUserName";
        final var errorMessage = "User with username or email '" + userNameOrEmail + "' not found.";
        when(repository.findByUserNameOrEmail(userNameOrEmail)).thenReturn(Optional.empty());

        final ThrowableAssert.ThrowingCallable executable = () -> service.getByUserNameOrEmail(userNameOrEmail);
        final var exception = catchThrowableOfType(EntityNotFoundException.class, executable);

        assertThat(exception.getMessage()).isEqualTo(errorMessage);
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getById_returnUser_whenFound() {
        final var given = 44L;
        when(repository.findById(given)).thenReturn(Optional.of(tester));

        final var result = service.getById(given);

        assertThat(result).hasValue(tester);
        verify(repository).findById(given);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getUserDtoById_returnUserDto_whenFound() {
        final var given = 44L;
        final var expectedDto = testDataFactory.createUserDto();
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

        assertThat(exception.getMessage()).isEqualTo("User with id '" + givenId + "' not found.");
        verify(repository).findById(givenId);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper);
    }

    @Test
    void getAllUsers_returnListOfUsers_whenFound() {
        final var tester2 = testDataFactory.createTestUser("Meriadok", "Brandybuck", "Merry", "merry@shire.me");
        final var users = List.of(tester, tester2);
        final var pippinDto = testDataFactory.createUserDto();
        final var merryDto = new UserDto(45L, "Meriadok", "Brandybuck", "Merry", "merry@shire.me");
        final var expected = List.of(pippinDto, merryDto);
        when(repository.findAll()).thenReturn(users);
        when(mapper.toDto(tester)).thenReturn(pippinDto);
        when(mapper.toDto(tester2)).thenReturn(merryDto);

        final var result = service.getAllUsers();

        assertThat(result).isEqualTo(expected);
        verify(repository).findAll();
        verify(mapper).toDto(tester);
        verify(mapper).toDto(tester2);
        verifyNoMoreInteractions(repository, mapper);
    }

}
