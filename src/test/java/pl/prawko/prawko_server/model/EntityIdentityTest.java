package pl.prawko.prawko_server.model;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.List;
import java.util.function.LongFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class EntityIdentityTest {

    static Stream<Named<LongFunction<Object>>> entities() {
        return Stream.of(
                Named.of("Answer", id -> new Answer().setId(id)),
                Named.of("AnswerTranslation", id -> new AnswerTranslation().setId(id)),
                Named.of("Category", id -> new Category().setId(id)),
                Named.of("Exam", id -> new Exam().setId(id)),
                Named.of("Language", id -> new Language().setId(id)),
                Named.of("Question", id -> new Question().setId(id)),
                Named.of("QuestionTranslation", id -> new QuestionTranslation().setId(id)),
                Named.of("User", id -> new User().setId(id))
        );
    }

    @ParameterizedTest
    @MethodSource("entities")
    void equals_matchOnlySameInstance_whenTransient(final LongFunction<Object> factory) {
        final var entity = factory.apply(0);

        assertThat(entity).isEqualTo(entity);
        assertThat(entity).isNotEqualTo(factory.apply(0));
        assertThat(entity).isNotEqualTo(null);
    }

    @ParameterizedTest
    @MethodSource("entities")
    void equals_compareById_whenPersisted(final LongFunction<Object> factory) {
        final var entity = factory.apply(1);

        assertThat(entity).isEqualTo(factory.apply(1));
        assertThat(entity).hasSameHashCodeAs(factory.apply(1));
        assertThat(entity).isNotEqualTo(factory.apply(2));
    }

    @ParameterizedTest
    @MethodSource("entities")
    void hashCode_returnSameValue_whenIdDiffers(final LongFunction<Object> factory) {
        assertThat(factory.apply(0)).hasSameHashCodeAs(factory.apply(5));
    }

    @ParameterizedTest
    @MethodSource("entities")
    void toString_notThrow_whenAssociationsAreNotSet(final LongFunction<Object> factory) {
        assertThatNoException().isThrownBy(() -> factory.apply(0).toString());
    }

    @Test
    void hashCode_keepEntityInHashSet_whenIdIsAssignedAfterAdding() {
        final var user = new User();
        final var set = new HashSet<User>();
        set.add(user);

        user.setId(5);

        assertThat(set).contains(user);
    }

    @Test
    void equals_returnFalse_whenOtherEntityTypeHasSameId() {
        assertThat(new User().setId(1)).isNotEqualTo(new Exam().setId(1));
    }

    @Test
    void toString_notRecurse_whenUserAndExamReferenceEachOther() {
        final var user = new User().setId(1).setUserName("pippin").setPassword("secret-hash");
        final var category = new Category().setId(2).setName("B");
        final var exam = new Exam().setId(3).setUser(user).setCategory(category);
        category.setExams(List.of(exam));
        user.setExams(List.of(exam));

        assertThat(user.toString())
                .contains("id=1", "userName='pippin'")
                .doesNotContain("secret-hash", "password");
        assertThat(exam.toString()).contains("user=1", "category=B");
        assertThat(category.toString()).contains("name='B'");
    }

    @Test
    void toString_notRecurse_whenQuestionAssociationsAreBidirectional() {
        final var language = new Language().setId(1).setCode(Language.PL);
        final var question = new Question().setId(10).setName("Q10");
        final var translation = new QuestionTranslation().setId(11).setQuestion(question).setLanguage(language);
        final var answer = new Answer().setId(12).setQuestion(question);
        final var answerTranslation = new AnswerTranslation(answer, language, "Tak").setId(13);
        final var exam = new Exam().setId(14).setQuestions(List.of(question));
        answer.setTranslations(List.of(answerTranslation));
        question.setTranslations(List.of(translation))
                .setAnswers(List.of(answer))
                .setExams(List.of(exam));

        assertThat(question.toString()).contains("id=10", "name='Q10'");
        assertThat(translation.toString()).contains("question=10", "language=pl");
        assertThat(answer.toString()).contains("question=10");
        assertThat(answerTranslation.toString()).contains("answer=12", "language=pl");
        assertThat(exam.toString()).contains("id=14");
    }

    @Test
    void toString_includeRole_whenUserHasRole() {
        final var user = new User().setId(2).setRole(Role.ADMIN);

        assertThat(user.toString()).contains("id=2", "role=ADMIN");
    }

}
