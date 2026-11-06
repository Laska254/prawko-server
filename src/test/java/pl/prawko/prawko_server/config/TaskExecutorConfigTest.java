package pl.prawko.prawko_server.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import pl.prawko.prawko_server.PrawkoServerApplication;
import pl.prawko.prawko_server.repository.UserRepository;
import pl.prawko.prawko_server.service.IPasswordResetService;
import pl.prawko.prawko_server.test_data.UserTestData;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Starts its own context instead of using {@link IntegrationTest}, as closing the context is what's being tested.
 */
class TaskExecutorConfigTest {

    private static final long BUSY_MS = 1000;

    @Test
    void closingContext_sendQueuedEmail_whenPoolIsBusy() {
        final var mailSender = mock(MailSender.class);
        final var user = UserTestData.createTestUserPippin();
        try (final var context = new SpringApplicationBuilder(PrawkoServerApplication.class)
                .profiles("test")
                .registerShutdownHook(false)
                .initializers(initialized -> initialized.getBeanFactory().registerSingleton("mailSender", mailSender))
                .run("--server.port=0", "--spring.datasource.url=jdbc:h2:mem:shutdown")) {
            context.getBean(UserRepository.class).save(user);
            final var taskExecutor = context.getBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
                    ThreadPoolTaskExecutor.class);
            for (int thread = 0; thread < taskExecutor.getMaxPoolSize(); thread++) {
                taskExecutor.execute(TaskExecutorConfigTest::stayBusy);
            }
            context.getBean(IPasswordResetService.class).requestReset(user.getEmail());
        }
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    private static void stayBusy() {
        try {
            TimeUnit.MILLISECONDS.sleep(BUSY_MS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

}
