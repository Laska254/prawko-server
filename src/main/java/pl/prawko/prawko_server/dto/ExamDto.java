package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for exam responses.
 *
 * <p>Contains complete exam information including questions, answers, and score tracking.
 *
 */
@Schema(description = "Exam information")
public record ExamDto(

        @Schema(description = "Unique exam identifier")
        long id,

        @Schema(description = "ID of the user taking the exam")
        long userId,

        @Schema(description = "Exam creation timestamp")
        LocalDateTime created,

        @Schema(description = "Last exam update timestamp")
        LocalDateTime updated,

        @Schema(description = "List of questions in the exam")
        List<QuestionDto> questions,

        @Schema(description = "List of user's answers to exam questions")
        List<AnswerDto> userAnswers,

        @Schema(description = "User's score on the exam")
        int score,

        @Schema(description = "Whether the exam is currently active")
        boolean active

) {
}
