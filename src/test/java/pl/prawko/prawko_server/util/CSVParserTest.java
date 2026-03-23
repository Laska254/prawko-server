package pl.prawko.prawko_server.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartException;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCSV;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.service.implementation.CategoryService;
import pl.prawko.prawko_server.service.implementation.LanguageService;
import pl.prawko.prawko_server.test_data.AnswerTestData;
import pl.prawko.prawko_server.test_data.CategoryTestData;
import pl.prawko.prawko_server.test_data.LanguageTestData;
import pl.prawko.prawko_server.test_data.QuestionCSVTestData;
import pl.prawko.prawko_server.test_data.QuestionTestData;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CSVParserTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private LanguageService languageService;

    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private CSVParser parser;

    @Test
    void parseFileToQuestions_mapCSVFile_correctly() throws IOException {
        final var resource = new ClassPathResource("test_question.csv");
        final var inputStream = resource.getInputStream();
        final var file = new MockMultipartFile("file", "test_question.csv", "text/csv", inputStream);
        final var expected = List.of(QuestionTestData.createQuestion(QuestionType.BASIC), QuestionTestData.createQuestion(QuestionType.SPECIAL));

        when(categoryService.findAllFromString("A,B")).thenReturn(List.of(CategoryTestData.CATEGORY_A, CategoryTestData.CATEGORY_B));
        when(categoryService.findAllFromString("PT")).thenReturn(List.of(CategoryTestData.CATEGORY_PT));
        when(languageService.findAll()).thenReturn(LanguageTestData.ALL);
        when(questionMapper.toEntity(any())).thenAnswer(invocation -> {
            final QuestionCSV csv = invocation.getArgument(0);
            return new Question()
                    .setId(csv.id())
                    .setName(csv.name())
                    .setType(QuestionType.ofType(csv.type()))
                    .setMedia(csv.mediaName().replaceAll("\\.wmv$", ".webm"))
                    .setPoints(csv.value());
        });

        final var result = parser.parse(file);

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

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(MultipartException.class)
                .hasMessage("Invalid file format.");
    }

    @Test
    void fromQuestionCSVToAnswers_correctlyMapBasicAnswers() {
        final var question = QuestionTestData.createQuestion(QuestionType.BASIC);
        final var given = QuestionCSVTestData.createBasicQuestionCSV();
        final var expected = List.of(
                AnswerTestData.noAnswer(),
                AnswerTestData.yesAnswer()
        );

        final var result = parser.fromQuestionCSVToAnswers(given, question);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void fromQuestionsCSVToAnswers_correctlyMapSpecialAnswers() {
        final var given = QuestionCSVTestData.createSpecialQuestionCSV();
        final var question = QuestionTestData.createQuestion(QuestionType.SPECIAL);
        final var languages = LanguageTestData.ALL;
        final var expected = List.of(
                AnswerTestData.answerA(),
                AnswerTestData.answerB(),
                AnswerTestData.answerC()
        );
        when(languageService.findAll()).thenReturn(languages);

        final var result = parser.fromQuestionCSVToAnswers(given, question);

        assertThat(result).isEqualTo(expected);
    }

}
