package pl.prawko.prawko_server.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.List;
import java.util.function.LongFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class EntityIdentityTest {

    static Stream<Arguments> entities() {
        return Stream.of(
                Arguments.of(Named.of("Answer", id -> new Answer().setId(id))),
                Arguments.of(Named.of("AnswerTranslation", id -> new AnswerTranslation().setId(id))),
                Arguments.of(Named.of("Category", id -> new Category().setId(id))),
                Arguments.of(Named.of("Exam", id -> new Exam().setId(id))),
                Arguments.of(Named.of("Language", id -> new Language().setId(id))),
                Arguments.of(Named.of("Question", id -> new Question().setId(id))),
                Arguments.of(Named.of("QuestionTranslation", id -> new QuestionTranslation().setId(id))),
                Arguments.of(Named.of("User", id -> new User().setId(id)))
        );
    }

    @ParameterizedTest
    @MethodSource("entities")
    void shouldBeEqualOnlyToItselfWhenTransient(final Named factory) {
        final var entity = factory.create(0);

        assertThat(entity).isEqualTo(entity);
        assertThat(entity).isNotEqualTo(factory.create(0));
        assertThat(entity).isNotEqualTo(null);
    }

    @ParameterizedTest
    @MethodSource("entities")
    void shouldCompareByIdWhenPersisted(final Named factory) {
        final var entity = factory.create(1);

        assertThat(entity).isEqualTo(factory.create(1));
        assertThat(entity).hasSameHashCodeAs(factory.create(1));
        assertThat(entity).isNotEqualTo(factory.create(2));
    }

    @ParameterizedTest
    @MethodSource("entities")
    void shouldHaveSameHashCodeRegardlessOfId(final Named factory) {
        assertThat(factory.create(0)).hasSameHashCodeAs(factory.create(5));
    }

    @ParameterizedTest
    @MethodSource("entities")
    void shouldPrintWithoutAssociationsSet(final Named factory) {
        assertThatNoException().isThrownBy(() -> factory.create(0).toString());
    }

    @Test
    void shouldStayInHashSetAfterIdIsAssigned() {
        final var user = new User();
        final var set = new HashSet<User>();
        set.add(user);

        user.setId(5);

        assertThat(set).contains(user);
    }

    @Test
    void shouldNotBeEqualToOtherEntityTypeWithSameId() {
        assertThat(new User().setId(1)).isNotEqualTo(new Exam().setId(1));
    }

    @Test
    void shouldPrintBidirectionalUserExamAssociationWithoutRecursion() {
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
    void shouldPrintBidirectionalQuestionAssociationsWithoutRecursion() {
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
    void shouldPrintUserWithRole() {
        final var user = new User().setId(2).setRole(Role.ADMIN);

        assertThat(user.toString()).contains("id=2", "role=ADMIN");
    }

    private record Named(String name, LongFunction<Object> factory) {

        static Named of(final String name, final LongFunction<Object> factory) {
            return new Named(name, factory);
        }

        Object create(final long id) {
            return factory.apply(id);
        }

        @Override
        public String toString() {
            return name;
        }

    }

}
