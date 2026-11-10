package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.implementation.EmailVerificationService;
import pl.prawko.prawko_server.service.implementation.PasswordResetService;
import pl.prawko.prawko_server.testdata.UserTestData;

import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Runs the services that issue emailed tokens ({@link PasswordResetService}, {@link EmailVerificationService}) in real
 * transactions, letting a concurrent request commit right after their lookup by email.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TokenRequestConcurrencyTest {

    private final MailSender mailSender = mock(MailSender.class);

    @MockitoSpyBean
    private UserRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private PasswordResetService passwordResetService;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(repository, mock(PasswordEncoder.class),
                mock(IRefreshTokenService.class), mailSender, UserTestData.PASSWORD_RESET_URL,
                UserTestData.PASSWORD_RESET_TOKEN_VALIDITY, UserTestData.PASSWORD_RESET_COOLDOWN, TestUtils.MAIL_FROM,
                TestUtils.CLOCK);
        emailVerificationService = new EmailVerificationService(repository, mailSender,
                UserTestData.EMAIL_VERIFICATION_URL, UserTestData.EMAIL_VERIFICATION_TOKEN_VALIDITY,
                UserTestData.EMAIL_VERIFICATION_COOLDOWN, TestUtils.MAIL_FROM, TestUtils.CLOCK);
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void requestReset_storeTokenAndSendMail_whenIdentityIsUnlinkedConcurrently() {
        final var pippin = repository.save(UserTestData.createTestUserPippin()
                .addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID));
        changeAfterLookup(pippin, user -> user.removeIdentity(IdentityProvider.GOOGLE));

        passwordResetService.requestReset(pippin.getEmail());

        assertThat(repository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)).isEmpty();
        assertThat(repository.findById(pippin.getId()))
                .get().extracting(User::getPasswordResetTokenHash).isNotNull();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void requestReset_keepNewPasswordAndSendNoMail_whenPasswordIsChangedConcurrently() {
        final var pippin = repository.save(UserTestData.createTestUserPippin());
        changeAfterLookup(pippin, user -> user.setPassword(UserTestData.NEW_PASSWORD_HASH));

        assertThatThrownBy(() -> passwordResetService.requestReset(pippin.getEmail()))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        final var stored = repository.findById(pippin.getId()).orElseThrow();
        assertThat(stored.getPassword()).isEqualTo(UserTestData.NEW_PASSWORD_HASH);
        assertThat(stored.getPasswordResetTokenHash()).isNull();
        verifyNoInteractions(mailSender);
    }

    @Test
    void resendVerification_keepNewPasswordAndSendNoMail_whenPasswordIsChangedConcurrently() {
        final var pippin = repository.save(UserTestData.createUnverifiedPippin());
        changeAfterLookup(pippin, user -> user.setPassword(UserTestData.NEW_PASSWORD_HASH));

        assertThatThrownBy(() -> emailVerificationService.resendVerification(pippin.getEmail()))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        final var stored = repository.findById(pippin.getId()).orElseThrow();
        assertThat(stored.getPassword()).isEqualTo(UserTestData.NEW_PASSWORD_HASH);
        assertThat(stored.getVerificationTokenHash()).isEqualTo(pippin.getVerificationTokenHash());
        verifyNoInteractions(mailSender);
    }

    /**
     * Commits the change in a concurrent transaction right after the service looks the user up.
     */
    private void changeAfterLookup(final User user, final UnaryOperator<User> change) {
        final var realMethod = mockingDetails(repository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            final var stale = realMethod.answer(invocation);
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> repository.findById(user.getId())
                            .ifPresent(found -> repository.save(change.apply(found))));
            return stale;
        }).when(repository).findByEmailIgnoreCase(user.getEmail());
    }

}
