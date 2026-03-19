package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for question translation responses.
 *
 * <p>Represents a question translated into a specific language.
 *
 */
@Schema(description = "Question text translated into a specific language")
public record QuestionTranslationDto(

        @Schema(description = "Question content in the specified language")
        String content,

        @Schema(description = "Language code (ISO 639-1)")
        String languageCode

) {
}
