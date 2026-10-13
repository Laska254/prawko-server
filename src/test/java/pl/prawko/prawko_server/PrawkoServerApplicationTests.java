package pl.prawko.prawko_server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.context.ActiveProfiles;
import pl.prawko.prawko_server.service.implementation.ExamService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PrawkoServerApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void scheduledTasks_containFinishExpiredExams_whenContextLoads(@Autowired final ScheduledTaskHolder taskHolder) {
        assertThat(taskHolder.getScheduledTasks())
                .extracting(task -> task.getTask().toString())
                .contains(ExamService.class.getName() + ".finishExpiredExams");
    }

}
