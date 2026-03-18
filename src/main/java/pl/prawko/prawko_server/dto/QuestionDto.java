package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.prawko.prawko_server.model.QuestionType;

import java.util.List;

/**
 * DTO for question responses.
 *
 * <p>Contains question details including translations and possible answers.
 *
 */
@Schema(description = "Question details with translations and answers")
public record QuestionDto(

        @Schema(description = "Unique question identifier")
        long id,

        @Schema(description = "Question code/name")
        String name,

        @Schema(description = "Possible answers to the question")
        List<AnswerDto> answers,

        @Schema(description = "Media filename associated with the question")
        String media,

        @Schema(description = "Type of question")
        QuestionType type,

        @Schema(description = "Points awarded for correct answer")
        int value,

        @Schema(description = "Categories this question belongs to")
        List<String> categories,

        @Schema(description = "Translations of the question in different languages")
        List<QuestionTranslationDto> translations

) {
}
