package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * DTO for statistics of a user's finished exams.
 *
 * <p>Active exams are not counted. Without finished exams the counts are {@code 0}, {@code averageScore} and
 * {@code bestScore} are {@code null} and {@code categories} is empty.
 */
@Schema(description = "Statistics of user's finished exams, overall and per category")
public record UserStatsDto(

        @Schema(description = "Number of finished exams")
        long examsTaken,

        @Schema(description = "Number of finished exams with a passing score")
        long examsPassed,

        @Nullable
        @Schema(description = "Average score of finished exams, `null` without any", example = "63.5", nullable = true)
        Double averageScore,

        @Nullable
        @Schema(description = "Highest score of finished exams, `null` without any", example = "72", nullable = true)
        Integer bestScore,

        @Schema(description = "Statistics per category, only categories with finished exams")
        List<CategoryStatsDto> categories

) {
}
