package pl.prawko.prawko_server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.model.Exam;

/**
 * This class is responsible for mapping {@link Exam} model into {@link ExamDto} and vice versa.
 * <p>
 * The mapper is registered as a Spring {@link Component}, so it can be injected into services or other components that require answer mapping
 * functionality.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {QuestionMapper.class, AnswerMapper.class})
public interface ExamMapper {

    @Mapping(target = "userId", source = "user.id")
    ExamDto toDto(Exam entity);

}
