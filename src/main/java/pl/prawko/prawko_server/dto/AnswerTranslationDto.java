package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for answer translation responses.
 *
 * <p>Represents an answer translated into a specific language.
 *
 */
@Schema(description = "Answer text translated into a specific language")
public record AnswerTranslationDto(

        @Schema(description = "Answer content in the specified language")
        String content,

        @Schema(description = "Language code (ISO 639-1)")
        String languageCode

) {
}
