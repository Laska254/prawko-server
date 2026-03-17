package pl.prawko.prawko_server.dto;

import jakarta.validation.constraints.NotNull;
import pl.prawko.prawko_server.model.CategoryVariant;

public record CreateExamDto(

        @NotNull(message = "{createexam.userid.notnull}")
        Long userId,

        @NotNull(message = "{createexam.categoryname.notnull}")
        CategoryVariant categoryName

) {
}
