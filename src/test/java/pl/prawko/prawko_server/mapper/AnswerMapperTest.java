package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.Test;
import pl.prawko.prawko_server.dto.AnswerDto;
import pl.prawko.prawko_server.dto.AnswerTranslationDto;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.AnswerTranslation;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.test_data.AnswerTestData;
import pl.prawko.prawko_server.test_data.AnswerTranslationsTestData;
import pl.prawko.prawko_server.test_data.AnswerVariant;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.prawko.prawko_server.test_data.LanguageTestData.PL;

class AnswerMapperTest {

    private final AnswerMapper mapper = new AnswerMapperImpl();

    @Test
    void toDto_mapAllFields_whenCorrectIsRevealed() {
        final var translations = AnswerTranslationsTestData.createAnswerTranslations(AnswerVariant.A);
        final var translationDtos = AnswerTranslationsTestData.createAnswerTranslationsDtos(AnswerVariant.A);
        final var given = new Answer()
                .setId(7L)
                .setCorrect(false)
                .setQuestion(new Question().setId(2143L))
                .setTranslations(translations);
        final var expected = new AnswerDto(7L, 2143L, false, translationDtos);

        final var result = mapper.toDto(given, true);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toDto_mapCorrectToNull_whenCorrectIsNotRevealed() {
        final var given = AnswerTestData.answerB().setQuestion(new Question().setId(2143L));

        final var result = mapper.toDto(given, false);

        assertThat(result.correct()).isNull();
        assertThat(result.questionId()).isEqualTo(given.getQuestion().getId());
        assertThat(result.translations()).hasSameSizeAs(given.getTranslations());
    }

    @Test
    void toTranslationDto_mapContentAndLanguageCode_whenBothAreSet() {
        final var content = "Co 60 minut.";
        final var lang = PL;
        final var given = new AnswerTranslation()
                .setLanguage(lang)
                .setContent(content);
        final var expected = new AnswerTranslationDto(content, lang.getCode());

        final var result = mapper.toTranslationDto(given);

        assertThat(result).isEqualTo(expected);
    }

}
