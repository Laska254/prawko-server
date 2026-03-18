package pl.prawko.prawko_server.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartException;
import pl.prawko.prawko_server.mapper.AnswerMapper;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.implementation.CategoryService;
import pl.prawko.prawko_server.service.implementation.LanguageService;
import pl.prawko.prawko_server.test_data.CategoryTestData;
import pl.prawko.prawko_server.test_data.LanguageTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CSVParserTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private LanguageService languageService;

    private CSVParser parser;

    @BeforeEach
    void setUp() {
        final var answerMapper = new AnswerMapper(languageService);
        final var questionMapper = new QuestionMapper(categoryService, languageService, answerMapper);
        parser = new CSVParser(questionMapper);
    }

    @Test
    void parseFileToQuestions_mapCSVFile_correctly() throws IOException {
        final var resource = new ClassPathResource("test_question.csv");
        final var inputStream = resource.getInputStream();
        final var file = new MockMultipartFile("file", "test_question.csv", "text/csv", inputStream);
        final var expected = List.of(QuestionTestData.createQuestion(QuestionType.BASIC), QuestionTestData.createQuestion(QuestionType.SPECIAL));

        when(categoryService.findAllFromString("A,B")).thenReturn(List.of(CategoryTestData.CATEGORY_A, CategoryTestData.CATEGORY_B));
        when(categoryService.findAllFromString("PT")).thenReturn(List.of(CategoryTestData.CATEGORY_PT));
        when(languageService.findAll()).thenReturn(LanguageTestData.ALL);

        final var result = parser.parseFileToQuestions(file);

        assertThat(resource)
                .satisfies(res -> {
                    assertThat(res.exists()).isTrue();
                    assertThat(res.isReadable()).isTrue();
                });
        assertThat(result).containsExactlyInAnyOrder(expected.toArray(new Question[0]));
    }

    @Test
    void parse_shouldThrowMultipartException_whenFileIsNotCSV() {
        final var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[]{});

        assertThatThrownBy(() -> parser.parseFileToQuestions(file))
                .isInstanceOf(MultipartException.class)
                .hasMessage("Invalid file format.");
    }

}
