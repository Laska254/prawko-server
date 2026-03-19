package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO for answer responses.
 *
 * <p>Represents a possible answer to a question with translations in different languages.
 *
 */
@Schema(description = "Question answer option with translations")
public record AnswerDto(

        @Schema(description = "Unique answer identifier")
        long id,

        @Schema(description = "ID of the question this answer belongs to")
        long questionId,

        @Schema(description = "Whether this is the correct answer")
        boolean correct,

        @Schema(description = "Translations of the answer in different languages")
        List<AnswerTranslationDto> translations

) {
}
