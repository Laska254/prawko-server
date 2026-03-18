package pl.prawko.prawko_server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.CreateExamDto;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.service.implementation.ExamService;

/**
 * REST controller for exam management operations.
 *
 * <p>Provides endpoints for creating and retrieving exams. Exams are generated
 * for users based on specified categories and contain randomized questions
 * for assessment purposes.
 *
 */
@Tag(name = "Exams", description = "Exams management endpoints")
@Validated
@RestController
@RequestMapping(ApiConstants.EXAMS_BASE_URL)
public class ExamController {

    private final ExamService service;

    public ExamController(ExamService service) {
        this.service = service;
    }

    /**
     * Creates a new exam for a user in a specified category.
     *
     * <p>The exam is populated with randomly selected questions from the specified
     * category and assigned to the requesting user.
     *
     * @param dto the {@link CreateExamDto} containing user ID and category name
     * @return a {@link ResponseEntity} with HTTP 201 Created and Location header pointing
     * to the newly created exam
     */
    @Operation(summary = "Create exam", description = "Creates a new exam for a user in the specified category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Exam created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid argument"),
            @ApiResponse(responseCode = "404", description = "User or category not found")
    })
    @PostMapping
    public ResponseEntity<Void> createExam(@RequestBody @Valid final CreateExamDto dto) {
        final var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(ApiConstants.BY_ID)
                .buildAndExpand(service.createExam(dto.userId(), dto.categoryName().name()))
                .toUri();
        return ResponseEntity.created(location).build();
    }

    /**
     * Retrieves an exam by its ID.
     *
     * @param id the unique identifier of the exam
     * @return a {@link ResponseEntity} containing the {@link ExamDto}
     */
    @Operation(summary = "Get exam by ID", description = "Retrieves an exam with all its associated questions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exam found"),
            @ApiResponse(responseCode = "404", description = "Exam not found"),
            @ApiResponse(responseCode = "400", description = "ID is negative or zero"),
    })

    @GetMapping(ApiConstants.BY_ID)
    public ResponseEntity<ExamDto> getExam(@PathVariable @Positive final long id) {
        return ResponseEntity.ok(service.getById(id));
    }

}
