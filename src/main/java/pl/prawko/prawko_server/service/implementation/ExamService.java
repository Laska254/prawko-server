package pl.prawko.prawko_server.service.implementation;

import jakarta.persistence.EntityNotFoundException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Implementation of {@link IExamService} that manages {@link Exam} entities.
 */
@Service
public class ExamService implements IExamService {

    private static final Logger log = LoggerFactory.getLogger(ExamService.class);

    private final ExamRepository repository;
    private final UserService userService;
    private final ExamGenerator examGenerator;
    private final CategoryService categoryService;
    private final ExamMapper examMapper;

    public ExamService(final ExamRepository repository,
                       final UserService userService,
                       final ExamGenerator examGenerator,
                       final CategoryService categoryService,
                       final ExamMapper examMapper) {
        this.repository = repository;
        this.userService = userService;
        this.examGenerator = examGenerator;
        this.categoryService = categoryService;
        this.examMapper = examMapper;
    }

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException if no user or category was found
     */
    @Override
    @Transactional
    public long createExam(final long userId, final String categoryName) {
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

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException if the exam with the given ID is not found
     */
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
