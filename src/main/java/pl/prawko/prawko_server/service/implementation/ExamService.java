package pl.prawko.prawko_server.service.implementation;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.mapper.ExamMapper;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.model.Exam;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.repository.ExamRepository;
import pl.prawko.prawko_server.service.IExamService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link IExamService} that manages an {@link Exam} entities.
 */
@Service
public class ExamService implements IExamService {

    private static final Logger log = LoggerFactory.getLogger(ExamService.class);

    /**
     * key - points value of questions
     * value - amount of questions
     */
    private static final Map<QuestionType, Map<Integer, Integer>> QUESTIONS_DISTRIBUTION =
            Map.ofEntries(
                    Map.entry(QuestionType.BASIC, Map.ofEntries(
                            Map.entry(1, 4),
                            Map.entry(2, 6),
                            Map.entry(3, 10)
                    )),
                    Map.entry(QuestionType.SPECIAL, Map.ofEntries(
                            Map.entry(1, 2),
                            Map.entry(2, 4),
                            Map.entry(3, 6)
                    ))
            );

    @NonNull
    private final ExamRepository repository;
    @NonNull
    private final UserService userService;
    @NonNull
    private final QuestionService questionService;
    @NonNull
    private final CategoryService categoryService;
    @NonNull
    private final ExamMapper examMapper;

    public ExamService(@NonNull final ExamRepository repository,
                       @NonNull final UserService userService,
                       @NonNull final QuestionService questionService,
                       @NonNull final CategoryService categoryService,
                       @NonNull final ExamMapper examMapper) {
        this.repository = repository;
        this.userService = userService;
        this.questionService = questionService;
        this.categoryService = categoryService;
        this.examMapper = examMapper;
    }

    /**
     * {@inheritDoc}
     *
     * @return Optional of created {@code exam} or empty if {@code user} or {@code category} have not been found.
     */
    @Override
    @Transactional
    public Optional<Exam> createExam(final long userId, @NonNull final String categoryName) {
        log.info("Creating exam for user '{}' and category '{}'", userId, categoryName);
        final var user = userService.getById(userId);
        final var category = categoryService.findByName(categoryName);
        final var questions = Stream.of(
                        generateQuestions(category, QuestionType.BASIC),
                        generateQuestions(category, QuestionType.SPECIAL))
                .flatMap(Collection::stream)
                .toList();
        final var exam = new Exam()
                .setUser(user)
                .setQuestions(questions)
                .setCategory(category)
                .setScore(0)
                .setActive(true)
                .setUserAnswers(Collections.emptyList());
        user.getExams().add(exam);
        repository.save(exam);
        log.info("Created exam for user '{}'", user.getUserName());
        return Optional.of(exam);
    }

    @Nullable
    @Override
    @Transactional
    public ExamDto getById(long examId) {
        log.info("Fetching exam by id: {}", examId);
        final var exam = repository.findById(examId)
                .orElseThrow(() -> {
                    final var message = "Exam with '" + examId + "' not found.";
                    log.warn(message);
                    return new EntityNotFoundException(message);
                });
        return examMapper.toDto(exam);
    }

    private List<Question> selectRandomQuestions(@NonNull final List<Question> questions, final int count) {
        log.debug("Shuffling {} questions and return {} random ones", questions.size(), count);
        var copy = new ArrayList<>(questions);
        Collections.shuffle(copy);
        final var chosen = copy.subList(0, Math.min(count, copy.size()));
        log.debug("Chosen {} questions", chosen.size());
        return chosen;
    }

    private List<Question> generateQuestions(@NonNull final Category category,
                                             @NonNull final QuestionType questionType) {
        log.debug("Fetching questions with category '{}' and type '{}'", category, questionType);
        final var distribution = QUESTIONS_DISTRIBUTION.get(questionType);
        final Map<Integer, List<Question>> questions = questionService.getAllByTypeAndCategory(questionType, category.getName())
                .stream()
                .collect(Collectors.groupingBy(Question::getPoints));
        return distribution.entrySet().stream()
                .flatMap(entry ->
                        selectRandomQuestions(questions.getOrDefault(entry.getKey(), Collections.emptyList()), entry.getValue())
                                .stream())
                .toList();
    }

}
