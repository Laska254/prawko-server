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
import java.util.concurrent.ThreadLocalRandom;
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
        final var poolSize = questions.size();
        final var amount = Math.min(count, poolSize);
        log.debug("Selecting {} random questions from {}", amount, poolSize);
        final var shuffledQuestions = new ArrayList<>(questions);
        final var random = ThreadLocalRandom.current();
        var currentIndex = 0;
        while (currentIndex < amount) {
            final var swapIndex = currentIndex + random.nextInt(poolSize - currentIndex);
            Collections.swap(shuffledQuestions, currentIndex, swapIndex);
            currentIndex++;
        }
        final var selectedQuestions = new ArrayList<>(shuffledQuestions.subList(0, amount));
        log.debug("Chosen {} questions", selectedQuestions.size());
        return selectedQuestions;
    }

}
