package pl.prawko.prawko_server.mapper;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionMapperTest {

//    @Mock
//    private CategoryService categoryService;
//
//    @Mock
//    private LanguageService languageService;
//
//    @InjectMocks
//    private QuestionMapper questionMapper;
//
//    private final List<Language> languages = LanguageTestData.ALL;
//
//    @BeforeEach
//    void setUp() {
//        final var answerMapper = new AnswerMapper(languageService);
//        questionMapper = new QuestionMapper(categoryService, languageService, answerMapper);
//        when(languageService.findAll()).thenReturn(languages);
//    }
//
//        @Test
//    void mapQuestionCSVToQuestion_returnBasicQuestion() {
//        final var categories = List.of(CategoryTestData.CATEGORY_A, CategoryTestData.CATEGORY_B);
//        when(categoryService.findAllFromString("A,B")).thenReturn(categories);
//
//        final var given = QuestionCSVTestData.createBasicQuestionCSV();
//        final var expected = QuestionTestData.createQuestion(QuestionType.BASIC);
//
//        final var result = parser.mapQuestionCSVToQuestion(given);
//
//        assertThat(result).isEqualTo(expected);
//    }
//
//    @Test
//    void mapQuestionCSVToQuestion_returnSpecialQuestion() {
//        final var category = CategoryTestData.CATEGORY_PT;
//        when(categoryService.findAllFromString("PT")).thenReturn(List.of(category));
//        final var given = QuestionCSVTestData.createSpecialQuestionCSV();
//        final var expected = QuestionTestData.createQuestion(QuestionType.SPECIAL);
//
//        final var result = parser.mapQuestionCSVToQuestion(given);
//
//        assertThat(result).isEqualTo(expected);
//    }

}
