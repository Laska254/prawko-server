package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for submitting an answer to a question of an active exam.
 *
 * <p>The question is determined by the answer, so a previous answer to the same question is replaced.
 *
 */
@Schema(description = "Answer chosen by the user for a question of their exam")
public record SubmitAnswerRequest(

        @NotNull(message = "{answerid.required}")
        @Schema(description = "ID of the chosen answer")
        Long answerId

) {
}
