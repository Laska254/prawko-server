package pl.prawko.prawko_server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.implementation.QuestionService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ExamGenerator {

    private static final Logger log = LoggerFactory.getLogger(ExamGenerator.class);

    private final QuestionService questionService;

    public ExamGenerator(final QuestionService questionService) {
        this.questionService = questionService;
    }

    public List<Question> generate(final Category category) {
        log.info("Preparing questions for exam, using category '{}'", category.getName());
        return Stream.of(QuestionType.BASIC, QuestionType.SPECIAL)
                .map(type -> selectQuestionsBy(category, type))
                .flatMap(Collection::stream)
                .toList();
    }

    private List<Question> selectQuestionsBy(final Category category,
                                             final QuestionType questionType) {
        log.debug("Fetching questions with category '{}' and type '{}'", category.getName(), questionType);
        final var questions = questionService.getAllByTypeAndCategory(questionType, category.getName())
                .stream()
                .collect(Collectors.groupingBy(Question::getPoints));
        return questionType.getDistribution().entrySet().stream()
                .flatMap(entry ->
                        selectRandomQuestions(questions.getOrDefault(entry.getKey(), Collections.emptyList()), entry.getValue())
                                .stream())
                .toList();
    }

    private List<Question> selectRandomQuestions(final List<Question> questions, final int count) {
        log.debug("Shuffling {} questions and return {} random ones", questions.size(), count);
        var copy = new ArrayList<>(questions);
        Collections.shuffle(copy);
        final var chosen = copy.subList(0, Math.min(count, copy.size()));
        log.debug("Chosen {} questions", chosen.size());
        return chosen;
    }

}
