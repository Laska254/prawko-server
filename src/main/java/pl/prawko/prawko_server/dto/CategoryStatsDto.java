package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.prawko.prawko_server.model.CategoryVariant;

/**
 * DTO for statistics of a user's finished exams in one category.
 *
 * <p>Built directly by an aggregate query, so it only exists for categories with at least one finished exam.
 */
@Schema(description = "Statistics of user's finished exams in one category")
public record CategoryStatsDto(

        @Schema(description = "Driving license category of the exams")
        CategoryVariant category,

        @Schema(description = "Number of finished exams")
        long examsTaken,

        @Schema(description = "Number of finished exams with a passing score")
        long examsPassed,

        @Schema(description = "Average score of finished exams", example = "64.1")
        double averageScore,

        @Schema(description = "Highest score of finished exams", example = "72")
        int bestScore

) {
}
