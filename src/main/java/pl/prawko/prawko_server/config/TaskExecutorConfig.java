package pl.prawko.prawko_server.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keeps the database available while the application task executor finishes queued tasks on shutdown, as some of them
 * look up a user before sending an email.
 * <p>
 * The executor is created before the entity manager factory to serve as Spring's bootstrap executor, so by default
 * it's destroyed after it. This only reorders the destruction, a {@code @DependsOn} would also change the startup order.
 */
@Configuration
public class TaskExecutorConfig {

    private static final String ENTITY_MANAGER_FACTORY_BEAN_NAME = "entityManagerFactory";

    @Bean
    public static BeanFactoryPostProcessor destroyTaskExecutorBeforeEntityManagerFactory() {
        return beanFactory -> beanFactory.registerDependentBean(ENTITY_MANAGER_FACTORY_BEAN_NAME,
                TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
    }

}
