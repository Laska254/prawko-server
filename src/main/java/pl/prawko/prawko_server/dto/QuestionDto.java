package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;
import pl.prawko.prawko_server.model.CategoryVariant;
import pl.prawko.prawko_server.model.MediaType;
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

        @Nullable
        @Schema(description = "Absolute URL of the question's image or video, null without media", nullable = true)
        String mediaUrl,

        @Nullable
        @Schema(description = "Kind of the question's media, null without media", nullable = true)
        MediaType mediaType,

        @Schema(description = "Type of question")
        QuestionType type,

        @Schema(description = "Points awarded for correct answer")
        int value,

        @Schema(description = "Categories this question belongs to")
        List<CategoryVariant> categories,

        @Schema(description = "Translations of the question in different languages")
        List<QuestionTranslationDto> translations

) {
}
