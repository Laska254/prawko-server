package pl.prawko.prawko_server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

@Tag(name = "Questions", description = "Questions management endpoints")
@Validated
@RestController
@RequestMapping(ApiConstants.QUESTIONS_BASE_URL)
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Operation(summary = "Upload questions via csv file")
    @ApiResponse(responseCode = "201", description = "Questions from file added successfully")
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

    @Operation(summary = "Get question by ID")
    @ApiResponse(responseCode = "200", description = "Question found")
    @GetMapping(ApiConstants.BY_ID)
    public ResponseEntity<QuestionDto> getQuestion(@PathVariable @Positive final long id) {
        return ResponseEntity.ok(questionService.getById(id));
    }

    @Operation(summary = "Get all questions")
    @ApiResponse(responseCode = "200", description = "List of all questions")
    @GetMapping
    public ResponseEntity<List<QuestionDto>> getAllQuestions() {
        return ResponseEntity.ok(questionService.getAll());
    }

}
