package pl.prawko.prawko_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import pl.prawko.prawko_server.model.CategoryVariant;

/**
 * DTO for exam creation requests.
 *
 * <p>Specifies which user should take the exam and which driving license category
 * the exam should focus on.
 *
 */
@Schema(description = "Request to create a new exam for a user")
public record CreateExamDto(

        @NotNull(message = "{userid.required}")
        @Schema(description = "ID of the user taking the exam")
        Long userId,

        @NotNull(message = "{categoryname.required}")
        @Schema(description = "Driving license category for the exam")
        CategoryVariant categoryName

) {
}
