package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.prawko.prawko_server.model.CategoryVariant;

import java.time.Instant;

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
        CategoryVariant category,

        @Schema(description = "Exam creation timestamp (UTC)", example = "2026-10-23T12:00:00Z")
        Instant created,

        @Schema(description = "Last exam update timestamp (UTC)", example = "2026-10-23T12:10:00Z")
        Instant updated,

        @Schema(description = "Deadline for answers (UTC), `created` + exam duration", example = "2026-10-23T12:25:00Z")
        Instant expires,

        @Schema(description = "User's score on the exam")
        int score,

        @Schema(description = "Whether the exam is currently active")
        boolean active

) {
}
