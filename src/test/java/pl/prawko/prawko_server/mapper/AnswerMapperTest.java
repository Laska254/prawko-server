package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import pl.prawko.prawko_server.dto.AnswerTranslationDto;
import pl.prawko.prawko_server.model.AnswerTranslation;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.test_data.AnswerTestData;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.prawko.prawko_server.test_data.LanguageTestData.PL;

@SpringJUnitConfig(classes = AnswerMapperImpl.class)
class AnswerMapperTest {

    @Autowired
    private AnswerMapper mapper;

    @Nested
    class ToDto {

        @Test
        void correctlyMapsAllFields() {
            final var given = AnswerTestData.answerA()
                    .setQuestion(new Question().setId(2143L));
            final var expected = AnswerTestData.createAnswerDtoA();

            final var result = mapper.toDto(given);

            assertThat(result).isEqualTo(expected);
        }

    }

    @Nested
    class ToTranslationDto {

        @Test
        void correctlyMapAnswerTranslation_toDto() {
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

}
