package pl.prawko.prawko_server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * DTO for answer responses.
 *
 * <p>Represents a possible answer to a question with translations in different languages.
 * Whether the answer is correct is revealed only when allowed (e.g. not during an active exam),
 * otherwise {@code correct} is {@code null} and omitted from the response.
 *
 */
@Schema(description = "Question answer option with translations")
public record AnswerDto(

        @Schema(description = "Unique answer identifier")
        long id,

        @Schema(description = "ID of the question this answer belongs to")
        long questionId,

        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "Whether this is the correct answer, omitted when it must stay hidden", nullable = true)
        Boolean correct,

        @Schema(description = "Translations of the answer in different languages")
        List<AnswerTranslationDto> translations

) {
}
