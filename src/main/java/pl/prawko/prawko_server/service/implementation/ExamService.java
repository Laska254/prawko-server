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
import pl.prawko.prawko_server.model.Exam;
import pl.prawko.prawko_server.repository.ExamRepository;
import pl.prawko.prawko_server.service.IExamService;
import pl.prawko.prawko_server.util.ExamGenerator;

import java.util.Collections;

/**
 * Implementation of {@link IExamService} that manages an {@link Exam} entities.
 */
@Service
public class ExamService implements IExamService {

    private static final Logger log = LoggerFactory.getLogger(ExamService.class);

    @NonNull
    private final ExamRepository repository;
    @NonNull
    private final UserService userService;
    @NonNull
    private final ExamGenerator examGenerator;
    @NonNull
    private final CategoryService categoryService;
    @NonNull
    private final ExamMapper examMapper;

    public ExamService(@NonNull final ExamRepository repository,
                       @NonNull final UserService userService,
                       @NonNull final ExamGenerator examGenerator,
                       @NonNull final CategoryService categoryService,
                       @NonNull final ExamMapper examMapper) {
        this.repository = repository;
        this.userService = userService;
        this.examGenerator = examGenerator;
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
    public long createExam(final long userId, @NonNull final String categoryName) {
        log.info("Creating exam for user '{}' and category '{}'", userId, categoryName);
        final var user = userService.getById(userId);
        final var category = categoryService.findByName(categoryName);
        final var questions = examGenerator.generate(category);
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
        return exam.getId();
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

}
