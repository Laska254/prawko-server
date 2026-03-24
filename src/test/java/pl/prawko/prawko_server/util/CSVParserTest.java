package pl.prawko.prawko_server.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartException;
import pl.prawko.prawko_server.model.QuestionType;
import pl.prawko.prawko_server.test_data.QuestionTestData;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CSVParserTest {

    @Mock
    private QuestionCsvMapper questionCsvMapper;

    @InjectMocks
    private CSVParser csvParser;

    @Nested
    class Parse {

        @Test
        void successfullyParsesCSVFile() throws IOException {
            final var resource = new ClassPathResource("test_question.csv");
            final var file = new MockMultipartFile(
                    "file", "test_question.csv", "text/csv", resource.getInputStream());
            final var expected = List.of(
                    QuestionTestData.createQuestion(QuestionType.BASIC),
                    QuestionTestData.createQuestion(QuestionType.SPECIAL));

            when(questionCsvMapper.CSVsToEntities(any())).thenReturn(expected);

            final var result = csvParser.parse(file);

            assertThat(result).isEqualTo(expected);
            verify(questionCsvMapper).CSVsToEntities(any());
            verifyNoMoreInteractions(questionCsvMapper);
        }

        @Test
        void throwMultipartException_whenFileIsNotCSV() {
            final var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[]{});

            assertThatThrownBy(() -> csvParser.parse(file))
                    .isInstanceOf(MultipartException.class)
                    .hasMessage("Invalid file format.");
        }

    }

}