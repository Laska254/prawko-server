package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.NotNull;
import pl.prawko.prawko_server.model.CategoryVariant;

public record CreateExamDto(

        @NotNull(message = "userId is required")
        Long userId,

        @NotNull(message = "category is required")
        CategoryVariant categoryName

) {
}
