package pl.prawko.prawko_server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pl.prawko.prawko_server.dto.ApiResponse;
import pl.prawko.prawko_server.dto.QuestionDto;
import pl.prawko.prawko_server.service.implementation.QuestionService;

import java.util.List;

/**
 * REST controller to manage questions using HTTP requests.
 * <p>
 * Mapped on {@code /questions}
 */
@RestController
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * {@code POST} for uploading questions from a CSV file.
     * <p>
     * Delegate parsing of the file and saving parsed questions to {@link QuestionService}.
     * <p>
     * Errors are handled via {@link ExceptionController}.
     *
     * @param file provided file in request
     * @return success if questions from file were added
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> addQuestions(@RequestPart MultipartFile file) {
        final var questions = questionService.parseFileToQuestions(file);
        questionService.saveAll(questions);
        return new ApiResponse<>("Questions from file added successfully.");
    }

    @GetMapping("/{id}")
    public QuestionDto getQuestion(@PathVariable final long id) {
        return questionService.getById(id);
    }

    @GetMapping
    public List<QuestionDto> getAllQuestions() {
        return questionService.getAll();
    }

}
