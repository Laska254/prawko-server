package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO for exam history entries.
 *
 * <p>Contains basic exam information without questions and answers, suitable for listing user's exams.
 *
 */
@Schema(description = "Exam summary used in user's exam history")
public record ExamSummaryDto(

        @Schema(description = "Unique exam identifier")
        long id,

        @Schema(description = "Driving license category of the exam")
        String category,

        @Schema(description = "Exam creation timestamp")
        LocalDateTime created,

        @Schema(description = "Last exam update timestamp")
        LocalDateTime updated,

        @Schema(description = "User's score on the exam")
        int score,

        @Schema(description = "Whether the exam is currently active")
        boolean active

) {
}
