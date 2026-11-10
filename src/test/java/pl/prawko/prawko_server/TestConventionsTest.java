package pl.prawko.prawko_server;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.platform.commons.annotation.Testable;

import java.util.concurrent.TimeUnit;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Conventions for the test code: shared Spring context, no sleeps, test method names, map literals and enum constant
 * names.
 */
@AnalyzeClasses(packagesOf = PrawkoServerApplication.class, importOptions = ImportOption.OnlyIncludeTests.class)
class TestConventionsTest {

    @ArchTest
    static final ArchRule NO_DIRTIES_CONTEXT = noClasses()
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("org.springframework.test.annotation.DirtiesContext")
            .because("integration tests share one context and clean up their data in @AfterEach");

    @ArchTest
    static final ArchRule NO_SLEEP = noClasses()
            .that()
            .doNotHaveSimpleName("TaskExecutorConfigTest")
            .should()
            .callMethodWhere(target(name("sleep"))
                    .and(target(owner(type(Thread.class).or(type(TimeUnit.class))))))
            .because("async work is asserted with verify(mock, timeout(...)) or after(...);"
                    + " TaskExecutorConfigTest keeps the pool busy on purpose");

    @ArchTest
    static final ArchRule TEST_METHODS_ARE_NAMED_SUBJECT_RESULT_CONDITION = methods()
            .that()
            .areMetaAnnotatedWith(Testable.class)
            .and()
            .doNotHaveName("findAllByUser_IdAndCategory_Name_returnOnlyUserExamsOfCategory")
            .should()
            .haveNameMatching("[a-z][a-zA-Z0-9]*(_[a-z][a-zA-Z0-9]*){1,2}")
            .andShould()
            .haveNameNotStartingWith("should")
            .because("test names are subject_result_whenCondition, or result_whenCondition in a @Nested class");

    @ArchTest
    static final ArchRule NO_MAP_OF = ArchitectureTest.NO_MAP_OF;

    @ArchTest
    static final ArchRule ENUM_CONSTANTS_ARE_UPPER_SNAKE_CASE = ArchitectureTest.ENUM_CONSTANTS_ARE_UPPER_SNAKE_CASE;

}
