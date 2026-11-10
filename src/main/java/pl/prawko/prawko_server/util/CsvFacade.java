package pl.prawko.prawko_server.util;

import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCsv;
import pl.prawko.prawko_server.service.ICategoryService;

@Component
public class CsvFacade {

    private final ICategoryService categoryService;
    private final QuestionMapper questionMapper;
    private final QuestionTranslationFactory translationFactory;
    private final AnswerFactory answerFactory;

    public CsvFacade(final QuestionMapper questionMapper,
                     final QuestionTranslationFactory translationFactory,
                     final AnswerFactory answerFactory,
                     final ICategoryService categoryService) {
        this.questionMapper = questionMapper;
        this.translationFactory = translationFactory;
        this.answerFactory = answerFactory;
        this.categoryService = categoryService;
    }

    public Question mapSingleRow(final QuestionCsv questionCsv) {
        final var question = questionMapper.toEntity(questionCsv);
        question.setTranslations(translationFactory.createTranslations(questionCsv, question));
        question.setAnswers(answerFactory.create(questionCsv, question));
        question.setCategories(categoryService.findAllFromString(questionCsv.categories()));
        return question;
    }

}
