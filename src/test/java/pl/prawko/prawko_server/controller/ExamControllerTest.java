package pl.prawko.prawko_server.controller;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import pl.prawko.prawko_server.config.IntegrationTest;
import pl.prawko.prawko_server.config.TestUtils;
import pl.prawko.prawko_server.dto.CreateExamDto;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.model.CategoryVariant;
import pl.prawko.prawko_server.model.Exam;
import pl.prawko.prawko_server.model.User;
import pl.prawko.prawko_server.repository.ExamRepository;
import pl.prawko.prawko_server.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.prawko.prawko_server.config.TestUtils.BASE_URL;

@IntegrationTest
public class ExamControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    private static final String URL = "/exams";

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        final var mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        restClient = RestClient.builder()
                .baseUrl(BASE_URL + port)
                .messageConverters(converters -> converters.add(new MappingJackson2HttpMessageConverter(mapper)))
                .build();
    }

    @Test
    void createExam_returnCreated() {
        final var tester = userRepository.save(new User());
        final var dto = new CreateExamDto(tester.getId(), CategoryVariant.B);

        final var response = restClient.post()
                .uri(URL)
                .headers(TestUtils::authUser)
                .body(dto)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void getExam_returnExam_whenFound() {
        final var tester = userRepository.save(new User());
        final var exam = examRepository.save(new Exam().setUser(tester));

        final var response = restClient.get()
                .uri(URL + "/{id}", exam.getId())
                .headers(TestUtils::authUser)
                .retrieve()
                .body(ExamDto.class);

        assertThat(response.userId()).isEqualTo(exam.getUser().getId());
        assertThat(response.id()).isEqualTo(exam.getId());
    }

}
