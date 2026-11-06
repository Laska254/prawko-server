package pl.prawko.prawko_server.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import pl.prawko.prawko_server.test_data.UserTestData;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs {@link UserRepository#findWithLockById} in real transactions, as a lock needs a second transaction to contend
 * with.
 */
@DataJpaTest(properties = "spring.jpa.properties.jakarta.persistence.lock.timeout=100")
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UserRepositoryLockTest {

    @Autowired
    private UserRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void findWithLockById_throwPessimisticLockingFailure_whenAnotherTransactionHoldsTheRow() {
        final var id = repository.save(UserTestData.createTestUserPippin()).getId();
        final var contender = new TransactionTemplate(transactionManager);
        contender.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        new TransactionTemplate(transactionManager).executeWithoutResult(holder -> {
            repository.findWithLockById(id);

            assertThatThrownBy(() -> contender.executeWithoutResult(status -> repository.findWithLockById(id)))
                    .isInstanceOf(PessimisticLockingFailureException.class);
        });
    }

}
