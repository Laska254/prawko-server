package pl.prawko.prawko_server.dto;

import org.springframework.lang.NonNull;
import pl.prawko.prawko_server.model.QuestionTranslation;
import pl.prawko.prawko_server.model.QuestionType;

import java.util.List;

public record QuestionDto(

        long id,
        @NonNull String name,
        List<AnswerDto> answers,
        @NonNull String media,
        @NonNull QuestionType type,
        int value,
        List<String> categories,
        List<QuestionTranslation> translations

) {
}
