package pl.prawko.prawko_server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.prawko.prawko_server.constants.ApiConstants;
import pl.prawko.prawko_server.dto.QuestionDto;
import pl.prawko.prawko_server.service.implementation.QuestionService;

import java.util.List;

/**
 * REST controller for question management operations.
 *
 * <p>Provides endpoints for uploading questions via CSV files and retrieving
 * individual or all questions from the database.
 *
 */
@Tag(name = "Questions", description = "Questions management endpoints")
@Validated
@RestController
@RequestMapping(ApiConstants.QUESTIONS_BASE_URL)
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * Uploads and parses questions from a CSV file.
     *
     * <p>Accepts a multipart file containing questions in CSV format and imports
     * them into the database.
     *
     * @param file the multipart CSV file containing questions
     * @return a {@link ResponseEntity} with HTTP 201 Created
     */
    @Operation(summary = "Upload questions via CSV file", description = "Uploads a CSV file containing questions and imports them.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Questions from file added successfully"),
            @ApiResponse(responseCode = "400", description = "File is missing or invalid format"),
            @ApiResponse(responseCode = "415", description = "Unsupported media type")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> addQuestions(@RequestPart final MultipartFile file) {
        final var questions = questionService.parseFileToQuestions(file);
        questionService.saveAll(questions);
        final var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .buildAndExpand(questions)
                .toUri();
        return ResponseEntity.created(location).build();
    }

    /**
     * Retrieves a question by its ID.
     *
     * @param id the unique identifier of the question (must be positive)
     * @return a {@link ResponseEntity} containing the {@link QuestionDto}
     */
    @Operation(summary = "Get question by ID", description = "Retrieves a question with all its associated translations and answers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question found"),
            @ApiResponse(responseCode = "404", description = "Question not found"),
            @ApiResponse(responseCode = "400", description = "ID is negative or zero")
    })
    @GetMapping(ApiConstants.BY_ID)
    public ResponseEntity<QuestionDto> getQuestion(@PathVariable @Positive final long id) {
        return ResponseEntity.ok(questionService.getById(id));
    }

    /**
     * Retrieves all questions from database.
     *
     * @return a {@link ResponseEntity} containing a list of {@link QuestionDto}'s
     */
    @Operation(summary = "Get all questions", description = "Retrieves a list of all questions in the database.")
    @ApiResponse(responseCode = "200", description = "List of all questions")
    @GetMapping
    public ResponseEntity<List<QuestionDto>> getAllQuestions() {
        return ResponseEntity.ok(questionService.getAll());
    }

}
