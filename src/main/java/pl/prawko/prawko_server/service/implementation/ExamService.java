package pl.prawko.prawko_server.service.implementation;

import jakarta.persistence.EntityNotFoundException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.dto.ExamSummaryDto;
import pl.prawko.prawko_server.exception.ExamFinishedException;
import pl.prawko.prawko_server.mapper.ExamMapper;
import pl.prawko.prawko_server.model.Answer;
import pl.prawko.prawko_server.model.Exam;
import pl.prawko.prawko_server.repository.AnswerRepository;
import pl.prawko.prawko_server.repository.ExamRepository;
import pl.prawko.prawko_server.service.IExamService;
import pl.prawko.prawko_server.util.ExamGenerator;

import java.util.ArrayList;

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
    private final AnswerRepository answerRepository;

    public ExamService(final ExamRepository repository,
                       final UserService userService,
                       final ExamGenerator examGenerator,
                       final CategoryService categoryService,
                       final ExamMapper examMapper,
                       final AnswerRepository answerRepository) {
        this.repository = repository;
        this.userService = userService;
        this.examGenerator = examGenerator;
        this.categoryService = categoryService;
        this.examMapper = examMapper;
        this.answerRepository = answerRepository;
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
                .setUserAnswers(new ArrayList<>());
        user.getExams().add(exam);
        repository.save(exam);
        log.info("Created exam for user with id '{}'", userId);
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
    public ExamDto getById(final long examId) {
        log.info("Fetching exam by id: {}", examId);
        final var exam = findById(examId);
        return examMapper.toDto(exam, !exam.isActive());
    }

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException if the user with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ExamSummaryDto> getAllByUserId(final long userId, final Pageable pageable) {
        log.info("Fetching exams history of user with id: {}", userId);
        userService.getById(userId);
        final var exams = repository.findAllByUser_Id(userId, pageable)
                .map(examMapper::toSummaryDto);
        log.info("Found {} exam(s) for user with id: {}", exams.getTotalElements(), userId);
        return exams;
    }

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException if the exam is not found, or the answer doesn't belong to any of its questions
     * @throws AccessDeniedException   if the exam belongs to another user
     * @throws ExamFinishedException   if the exam is no longer active
     */
    @Override
    @Transactional
    public void submitAnswer(final long examId, final long userId, final long answerId) {
        log.info("Submitting answer '{}' to exam '{}' by user '{}'", answerId, examId, userId);
        final var exam = findActiveExamOfUser(examId, userId);
        final var answer = answerRepository.findById(answerId)
                .filter(found -> exam.getQuestions().contains(found.getQuestion()))
                .orElseThrow(() -> {
                    final var message = "Answer with '" + answerId + "' not found in exam '" + examId + "'.";
                    log.warn(message);
                    return new EntityNotFoundException(message);
                });
        exam.getUserAnswers().removeIf(previous -> previous.getQuestion().equals(answer.getQuestion()));
        exam.getUserAnswers().add(answer);
        log.info("Saved answer '{}' to exam '{}'", answerId, examId);
    }

    private Exam findById(final long examId) {
        return repository.findById(examId)
                .orElseThrow(() -> {
                    final var message = "Exam with '" + examId + "' not found.";
                    log.warn(message);
                    return new EntityNotFoundException(message);
                });
    }

    private Exam findActiveExamOfUser(final long examId, final long userId) {
        final var exam = findById(examId);
        if (exam.getUser().getId() != userId) {
            log.warn("User '{}' tried to modify exam '{}' of another user", userId, examId);
            throw new AccessDeniedException("Exam '" + examId + "' belongs to another user.");
        }
        if (!exam.isActive()) {
            final var message = "Exam with '" + examId + "' is already finished.";
            log.warn(message);
            throw new ExamFinishedException(message);
        }
        return exam;
    }

}
