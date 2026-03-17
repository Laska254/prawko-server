package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.NotNull;
import pl.prawko.prawko_server.model.CategoryVariant;

public record CreateExamDto(

        @NotNull(message = "{userid.required}")
        Long userId,

        @NotNull(message = "{categoryname.required}")
        CategoryVariant categoryName

) {
}
