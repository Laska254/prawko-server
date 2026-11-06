package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
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
import pl.prawko.prawko_server.service.implementation.PasswordResetService;
import pl.prawko.prawko_server.test_data.UserTestData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;

/**
 * Runs {@link PasswordResetService} in real transactions, letting a concurrent request commit right after its lookup.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PasswordResetServiceConcurrencyTest {

    @MockitoSpyBean
    private UserRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void requestReset_storeTokenAndSendMail_whenIdentityIsUnlinkedConcurrently() {
        final var pippin = repository.save(UserTestData.createTestUserPippin()
                .addIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID));
        final var mailSender = mock(MailSender.class);
        final var service = new PasswordResetService(repository, mock(PasswordEncoder.class),
                mock(IRefreshTokenService.class), mailSender, UserTestData.PASSWORD_RESET_URL,
                UserTestData.PASSWORD_RESET_TOKEN_VALIDITY, UserTestData.PASSWORD_RESET_COOLDOWN, TestUtils.MAIL_FROM,
                TestUtils.CLOCK);
        final var realMethod = mockingDetails(repository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            final var stale = realMethod.answer(invocation);
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> repository.findById(pippin.getId())
                            .map(user -> repository.save(user.removeIdentity(IdentityProvider.GOOGLE))));
            return stale;
        }).when(repository).findByEmailIgnoreCase(pippin.getEmail());

        service.requestReset(pippin.getEmail());

        assertThat(repository.findByIdentity(IdentityProvider.GOOGLE, UserTestData.PROVIDER_USER_ID)).isEmpty();
        assertThat(repository.findById(pippin.getId()))
                .get().extracting(User::getPasswordResetTokenHash).isNotNull();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

}
