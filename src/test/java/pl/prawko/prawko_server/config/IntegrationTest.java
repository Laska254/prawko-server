package pl.prawko.prawko_server.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
@MockitoBean(types = MailSender.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegrationTest {
}
