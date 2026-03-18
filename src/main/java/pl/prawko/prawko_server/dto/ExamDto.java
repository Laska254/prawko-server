package pl.prawko.prawko_server.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ExamDto(

        long id,
        long userId,
        LocalDateTime created,
        LocalDateTime updated,
        List<QuestionDto> questions,
        List<AnswerDto> userAnswers,
        int score,
        boolean active

) {
}
