package pl.prawko.prawko_server.dto;

import org.springframework.lang.NonNull;

public record QuestionTranslationDto(

        @NonNull String content,
        @NonNull String languageCode

) {
}
