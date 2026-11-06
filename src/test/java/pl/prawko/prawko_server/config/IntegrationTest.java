package pl.prawko.prawko_server.config;

import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import pl.prawko.prawko_server.repository.UserRepository;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@MockitoBean(types = MailSender.class)
@MockitoSpyBean(types = UserRepository.class)
@AutoConfigureMockRestServiceServer
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegrationTest {
}
