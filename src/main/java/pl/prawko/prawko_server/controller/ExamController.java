package pl.prawko.prawko_server.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.prawko.prawko_server.dto.ApiResponse;
import pl.prawko.prawko_server.dto.CreateExamDto;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.model.Exam;
import pl.prawko.prawko_server.service.implementation.ExamService;

/**
 * REST controller to manage exams using HTTP requests.
 * <p>
 * Mapped on {@code /exams}
 */
@RestController
@RequestMapping("/exams")
public class ExamController {

    private final ExamService service;

    public ExamController(ExamService service) {
        this.service = service;
    }

    /**
     * POST method to create a new {@link Exam} using {@link CreateExamDto}.
     *
     * @param dto the valid DTO
     * @return status 201 with success message
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createExam(@RequestBody @Valid final CreateExamDto dto) {
        service.createExam(dto.userId(), dto.categoryName().name())
                .orElseThrow(() -> new EntityNotFoundException("User or category not found."));
        return new ApiResponse<>("Exam successfully created.");
    }

    /**
     * GET method to get an {@link Exam} using {@code id}
     *
     * @param examId id of exam to get
     * @return {@code Exam} if found
     */
    @GetMapping("/{examId}")
    public ExamDto getExam(@PathVariable final long examId) {
        return service.getById(examId);
    }

}
