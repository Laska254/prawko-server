package pl.prawko.prawko_server.dto;

import pl.prawko.prawko_server.model.QuestionType;

import java.util.List;

public record QuestionDto(

        long id,
        String name,
        List<AnswerDto> answers,
        String media,
        QuestionType type,
        int value,
        List<String> categories,
        List<QuestionTranslationDto> translations

) {
}
