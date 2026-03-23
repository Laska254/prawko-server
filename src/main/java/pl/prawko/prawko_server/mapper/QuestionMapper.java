package pl.prawko.prawko_server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.dto.QuestionDto;
import pl.prawko.prawko_server.dto.QuestionTranslationDto;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCSV;
import pl.prawko.prawko_server.model.QuestionTranslation;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.implementation.CategoryService;

/**
 * This class is responsible for mapping {@link QuestionCSV} models into {@link Question} entities.
 * It's using {@link AnswerMapper} to delegate mapping of {@link Answer} entities linked to {@link Question}.
 * Categories are mapped using {@link CategoryService#findAllFromString(String)}.
 * <p>
 * The mapper is registered as a Spring {@link Component}, so it can be injected into services or other components that require question mapping
 * functionality.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = AnswerMapper.class,
        imports = QuestionType.class)
public interface QuestionMapper {

    @Mapping(target = "value", source = "points")
    QuestionDto toDto(Question question);

    @Mapping(target = "languageCode", source = "language.code")
    QuestionTranslationDto toTranslationDto(QuestionTranslation translation);

    @Mapping(target = "translations", ignore = true)
    @Mapping(target = "points", source = "value")
    @Mapping(target = "exams", ignore = true)
    @Mapping(target = "answers", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "media", expression = "java(questionCSV.mediaName().replaceAll(\"\\\\.wmv$\", \".webm\"))")
    @Mapping(target = "type", expression = "java(QuestionType.ofType(questionCSV.type()))")
    Question toEntity(QuestionCSV questionCSV);

    default String categoryToName(Category category) {
        return category.getName();
    }

}
