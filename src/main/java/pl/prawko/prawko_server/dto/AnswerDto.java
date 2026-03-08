package pl.prawko.prawko_server.dto;

import java.util.List;

public record AnswerDto(

        long id,
        long questionId,
        boolean correct,
        List<AnswerTranslationDto> translations

) {
}
