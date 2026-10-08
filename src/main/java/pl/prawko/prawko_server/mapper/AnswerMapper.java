package pl.prawko.prawko_server.mapper;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.dto.AnswerDto;
import pl.prawko.prawko_server.dto.AnswerTranslationDto;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.AnswerTranslation;

/**
 * This class is responsible for mapping {@link Answer} related entities and dto's.
 * <p>
 * The mapper is registered as a Spring {@link Component}, so it can be injected into services or other components that require answer mapping
 * functionality.
 * <p>
 * Whether an answer is correct is mapped only when {@code revealCorrect} is {@code true}, otherwise it's {@code null},
 * so the answer key isn't leaked to users solving an exam.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AnswerMapper {

    @Mapping(target = "questionId", source = "question.id")
    @Mapping(target = "correct", expression = "java(revealCorrect ? answer.isCorrect() : null)")
    AnswerDto toDto(Answer answer, @Context boolean revealCorrect);

    @Mapping(target = "languageCode", source = "language.code")
    AnswerTranslationDto toTranslationDto(AnswerTranslation translation);

}
