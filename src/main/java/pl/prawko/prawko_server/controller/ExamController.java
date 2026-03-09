package pl.prawko.prawko_server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.prawko.prawko_server.dto.CreateExamDto;
import pl.prawko.prawko_server.dto.ExamDto;
import pl.prawko.prawko_server.model.ApiConstants;
import pl.prawko.prawko_server.service.implementation.ExamService;

@Tag(name = "Exams", description = "Exams management endpoints")
@Validated
@RestController
@RequestMapping(ApiConstants.EXAMS_BASE_URL)
public class ExamController {

    private final ExamService service;

    public ExamController(ExamService service) {
        this.service = service;
    }

    @Operation(summary = "Create exam")
    @ApiResponse(responseCode = "201", description = "Exam created")
    @PostMapping
    public ResponseEntity<Void> createExam(@RequestBody @Valid final CreateExamDto dto) {
        final var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(ApiConstants.BY_ID)
                .buildAndExpand(service.createExam(dto.userId(), dto.categoryName().name()))
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Get exam by ID")
    @ApiResponse(responseCode = "200", description = "Exam found")
    @GetMapping(ApiConstants.BY_ID)
    public ResponseEntity<ExamDto> getExam(@PathVariable @Positive final long id) {
        return ResponseEntity.ok(service.getById(id));
    }

}
