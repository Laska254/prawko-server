package pl.prawko.prawko_server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.prawko.prawko_server.config.IsSelfOrAdmin;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.CreateExamDto;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.dto.ExamSummaryDto;
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
     * category and assigned to the requesting user. Allowed only for the user themselves or an admin.
     *
     * @param dto the {@link CreateExamDto} containing user ID and category name
     * @return a {@link ResponseEntity} with HTTP 201 Created and Location header pointing
     * to the newly created exam
     */
    @Operation(summary = "Create exam", description = "Creates a new exam for a user in the specified category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Exam created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid argument"),
            @ApiResponse(responseCode = "403", description = "Creating an exam for another user is not allowed"),
            @ApiResponse(responseCode = "404", description = "User or category not found")
    })
    @IsSelfOrAdmin(userId = "#dto.userId()")
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
     * <p>Allowed only for the owner of the exam or an admin.
     *
     * @param id the unique identifier of the exam
     * @return a {@link ResponseEntity} containing the {@link ExamDto}
     */
    @Operation(summary = "Get exam by ID", description = "Retrieves an exam with all its associated questions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exam found"),
            @ApiResponse(responseCode = "403", description = "Exam belongs to another user"),
            @ApiResponse(responseCode = "404", description = "Exam not found"),
            @ApiResponse(responseCode = "400", description = "ID is negative or zero"),
    })
    @PostAuthorize("hasRole('ADMIN') or principal.isSelf(returnObject.body.userId())")
    @GetMapping(ApiConstants.BY_ID)
    public ResponseEntity<ExamDto> getExam(@PathVariable @Positive final long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    /**
     * Retrieves a page of exams history of a user.
     *
     * <p>Single exam details can be retrieved using {@link #getExam(long)} with an ID from the returned page.
     * Sorted from the newest unless specified otherwise. Allowed only for the user themselves or an admin.
     *
     * @param userId   the unique identifier of the user (must be positive)
     * @param pageable the pagination and sorting information
     * @return a {@link ResponseEntity} containing a {@link PagedModel} of {@link ExamSummaryDto}'s
     */
    @Operation(summary = "Get user's exams history", description = "Retrieves a page of user's exams, by default ordered from the newest")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of user's exams"),
            @ApiResponse(responseCode = "403", description = "Accessing exams of another user is not allowed"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "User ID is missing, negative or zero, or invalid sort property"),
    })
    @IsSelfOrAdmin(userId = "#userId")
    @GetMapping
    public ResponseEntity<PagedModel<ExamSummaryDto>> getUserExams(
            @RequestParam @Positive final long userId,
            @ParameterObject
            @SortDefault(sort = {"created", "id"}, direction = Sort.Direction.DESC) final Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(service.getAllByUserId(userId, pageable)));
    }

}
