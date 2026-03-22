package pl.prawko.prawko_server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.dto.AnswerDto;
import pl.prawko.prawko_server.dto.AnswerTranslationDto;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.AnswerTranslation;
import pl.prawko.prawko_server.model.QuestionCSV;

/**
 * This class is responsible for mapping {@link QuestionCSV} model into {@link Answer} entity.
 * <p>
 * The mapper is registered as a Spring {@link Component}, so it can be injected into services or other components that require answer mapping
 * functionality.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AnswerMapper {

    @Mapping(target = "questionId", source = "question.id")
    AnswerDto toDto(Answer answer);

    @Mapping(target = "languageCode", source = "language.code")
    AnswerTranslationDto toTranslationDto(AnswerTranslation translation);

}
