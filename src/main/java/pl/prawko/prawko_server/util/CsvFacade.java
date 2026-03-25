package pl.prawko.prawko_server.util;

import org.springframework.stereotype.Component;
import pl.prawko.prawko_server.mapper.QuestionMapper;
import pl.prawko.prawko_server.model.Question;
import pl.prawko.prawko_server.model.QuestionCSV;
import pl.prawko.prawko_server.service.implementation.CategoryService;

@Component
public class CsvFacade {

    private final CategoryService categoryService;
    private final QuestionMapper questionMapper;
    private final QuestionTranslationFactory translationFactory;
    private final AnswerFactory answerFactory;

    public CsvFacade(final QuestionMapper questionMapper,
                     final QuestionTranslationFactory translationFactory,
                     final AnswerFactory answerFactory,
                     final CategoryService categoryService) {
        this.questionMapper = questionMapper;
        this.translationFactory = translationFactory;
        this.answerFactory = answerFactory;
        this.categoryService = categoryService;
    }

    public Question mapSingleRow(final QuestionCSV questionCSV) {
        final var question = questionMapper.toEntity(questionCSV);
        question.setTranslations(translationFactory.createTranslations(questionCSV, question));
        question.setAnswers(answerFactory.create(questionCSV, question));
        question.setCategories(categoryService.findAllFromString(questionCSV.categories()));
        return question;
    }

}
