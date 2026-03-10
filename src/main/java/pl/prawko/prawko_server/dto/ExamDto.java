package pl.prawko.prawko_server.dto;

import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.List;

public record ExamDto(

        long id,
        long userId,
        @NonNull LocalDateTime created,
        @NonNull LocalDateTime updated,
        List<QuestionDto> questions,
        List<AnswerDto> userAnswers,
        int score,
        boolean active

) {
}
