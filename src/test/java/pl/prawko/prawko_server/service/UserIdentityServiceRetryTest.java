package pl.prawko.prawko_server.service;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import pl.prawko.prawko_server.exception.AlreadyExistsException;
import pl.prawko.prawko_server.model.IdentityProvider;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.implementation.UserIdentityService;
import pl.prawko.prawko_server.test_data.GoogleTestData;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Runs {@link UserIdentityService} in real transactions, letting a concurrent request commit right after a lookup, as
 * if it won the race to the save.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(UserIdentityService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UserIdentityServiceRetryTest {

    private static final IdentityProvider PROVIDER = IdentityProvider.GOOGLE;

    @MockitoSpyBean
    private UserRepository repository;

    @MockitoBean
    private IIdentityNotificationService notificationService;

    @Autowired
    private IUserIdentityService service;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void resolveUser_signInUserCreatedConcurrently_whenSaveConflicts() {
        final var profile = UserTestData.createPippinProfile(PROVIDER);
        final var concurrent = UserTestData.createGooglePippin();
        doAnswer(invocation -> {
            inNewTransaction(() -> repository.save(concurrent));
            return Optional.empty();
        }).doAnswer(realMethod()).when(repository).findByEmailIgnoreCase(profile.email());

        final var result = service.resolveUser(profile);

        assertThat(result.getId()).isEqualTo(concurrent.getId());
        assertThat(repository.count()).isOne();
        verify(repository, times(2)).findByIdentity(PROVIDER, profile.providerUserId());
    }

    @Test
    void resolveUser_signInUser_whenSameAccountIsLinkedConcurrently() {
        final var pippin = repository.save(UserTestData.createTestUserPippin());
        final var profile = UserTestData.createPippinProfile(PROVIDER);
        doAnswer(invocation -> {
            final var stale = (Optional<?>) realMethod().answer(invocation);
            stale.map(User.class::cast)
                    .map(User::getIdentities)
                    .ifPresent(Hibernate::initialize);
            inNewTransaction(() -> repository.findById(pippin.getId())
                    .map(user -> repository.save(user.addIdentity(PROVIDER, profile.providerUserId()))));
            return stale;
        }).doAnswer(realMethod()).when(repository).findByEmailIgnoreCase(profile.email());

        final var result = service.resolveUser(profile);

        assertThat(result.getId()).isEqualTo(pippin.getId());
        verify(repository, times(2)).findByIdentity(PROVIDER, profile.providerUserId());
        assertThat(repository.findByIdentity(PROVIDER, profile.providerUserId()))
                .get().extracting(User::getId).isEqualTo(pippin.getId());
    }

    @Test
    void linkIdentity_throwAlreadyExists_whenSameAccountIsLinkedConcurrently() {
        final var pippin = repository.save(UserTestData.createTestUserPippin());
        final var profile = UserTestData.createPippinProfile(PROVIDER);
        doAnswer(invocation -> {
            final var free = realMethod().answer(invocation);
            inNewTransaction(() -> repository.save(UserTestData.createMerry()
                    .addIdentity(PROVIDER, profile.providerUserId())));
            return free;
        }).doAnswer(realMethod()).when(repository).findByIdentity(PROVIDER, profile.providerUserId());

        assertThatThrownBy(() -> service.linkIdentity(pippin.getId(), profile))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessage(GoogleTestData.LINKED_TO_OTHER_USER);

        verify(repository, times(2)).findByIdentity(PROVIDER, profile.providerUserId());
        verifyNoInteractions(notificationService);
    }

    /**
     * The spy's default answer, delegating to the repository proxy, whose methods {@code callRealMethod} can't reach.
     */
    private Answer<?> realMethod() {
        return mockingDetails(repository).getMockCreationSettings().getDefaultAnswer();
    }

    private void inNewTransaction(final Runnable action) {
        final var template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(status -> action.run());
    }

}
