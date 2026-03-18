package pl.prawko.prawko_server.mapper;

import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.model.Exam;

/**
 * This class is responsible for mapping {@link Exam} model into {@link ExamDto} and vice versa.
 * <p>
 * The mapper is registered as a Spring {@link Component}, so it can be injected into services or other components that require answer mapping
 * functionality.
 */
@Component
public class ExamMapper {

    private final QuestionMapper questionMapper;
    private final AnswerMapper answerMapper;

    public ExamMapper(final QuestionMapper questionMapper,
                      final AnswerMapper answerMapper) {
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
    }

    /**
     * Mapping to DTO.
     *
     * @param entity {@code Exam} to map
     * @return mapped {@code ExamDto}
     */
    public ExamDto toDto(final Exam entity) {
        final var questions = entity.getQuestions().stream()
                .map(questionMapper::toDto)
                .toList();
        final var userAnswers = entity.getUserAnswers().stream()
                .map(answerMapper::toDto)
                .toList();
        return new ExamDto(
                entity.getId(),
                entity.getUser().getId(),
                entity.getCreated(),
                entity.getUpdated(),
                questions,
                userAnswers,
                entity.getScore(),
                entity.isActive());
    }

}
