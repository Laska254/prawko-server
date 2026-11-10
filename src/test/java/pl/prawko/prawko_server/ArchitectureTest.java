package pl.prawko.prawko_server;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.rawParameterTypes;
import static com.tngtech.archunit.lang.conditions.ArchConditions.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture rules for the production code: layer access, package dependencies, naming and banned calls.
 * <p>
 * Tests are left out because they construct the service implementations on purpose; their rules live in
 * {@code TestConventionsTest}.
 * </p>
 */
@AnalyzeClasses(
        packagesOf = PrawkoServerApplication.class,
        importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludePackageInfos.class})
class ArchitectureTest {

    static final String APP = PrawkoServerApplication.class.getPackageName() + "..";

    @ArchTest
    static final ArchRule LAYERS = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage(APP)
            .layer("Controller")
            .definedBy("..controller..")
            .layer("Service")
            .definedBy("..service")
            .layer("ServiceImplementation")
            .definedBy("..service.implementation..")
            .layer("Repository")
            .definedBy("..repository..")
            .layer("Mapper")
            .definedBy("..mapper..")
            .layer("Util")
            .definedBy("..util..")
            .whereLayer("Controller")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Service")
            .mayOnlyBeAccessedByLayers("Controller", "ServiceImplementation", "Util")
            .whereLayer("ServiceImplementation")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Repository")
            .mayOnlyBeAccessedByLayers("ServiceImplementation")
            .whereLayer("Mapper")
            .mayOnlyBeAccessedByLayers("ServiceImplementation", "Util")
            .whereLayer("Util")
            .mayOnlyBeAccessedByLayers("ServiceImplementation");

    /** {@link #LAYERS} lets a layer access itself, so this covers one implementation injecting another. */
    @ArchTest
    static final ArchRule NO_FIELDS_OF_SERVICE_IMPLEMENTATIONS = noFields()
            .should()
            .haveRawType(resideInAPackage("..service.implementation.."))
            .because("inject the I*Service interfaces");

    @ArchTest
    static final ArchRule MODEL_DEPENDS_ON_NO_OTHER_PACKAGE = classes()
            .that()
            .resideInAPackage("..model..")
            .should()
            .onlyDependOnClassesThat(resideOutsideOfPackage(APP).or(resideInAPackage("..model..")));

    @ArchTest
    static final ArchRule DTOS_DEPEND_ONLY_ON_MODEL = classes()
            .that()
            .resideInAPackage("..dto..")
            .should()
            .onlyDependOnClassesThat(resideOutsideOfPackage(APP).or(resideInAnyPackage("..dto..", "..model..")));

    @ArchTest
    static final ArchRule RESPONSES_DO_NOT_DEPEND_ON_REQUESTS = noClasses()
            .that()
            .resideInAPackage("..dto.response..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..dto.request..");

    @ArchTest
    static final ArchRule REPOSITORIES_DO_NOT_DEPEND_ON_REQUESTS = noClasses()
            .that()
            .resideInAPackage("..repository..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..dto.request..");

    @ArchTest
    static final ArchRule EXCEPTIONS_DEPEND_ON_NO_OTHER_PACKAGE = classes()
            .that()
            .resideInAPackage("..exception..")
            .should()
            .onlyDependOnClassesThat(resideOutsideOfPackage(APP).or(resideInAPackage("..exception..")));

    @ArchTest
    static final ArchRule CONSTANTS_DEPEND_ON_NO_OTHER_PACKAGE = classes()
            .that()
            .resideInAPackage("..constants..")
            .should()
            .onlyDependOnClassesThat(resideOutsideOfPackage(APP).or(resideInAPackage("..constants..")));

    @ArchTest
    static final ArchRule REQUEST_BODIES_ARE_REQUESTS = methods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage("..controller..")
            .should(have(describe("@RequestBody parameters only from dto.request", method -> method.getParameters()
                    .stream()
                    .filter(parameter -> parameter.isAnnotatedWith(RequestBody.class))
                    .allMatch(parameter -> resideInAPackage("..dto.request..").test(parameter.getRawType())))));

    @ArchTest
    static final ArchRule REQUESTS_ARE_NAMED_REQUEST = classes()
            .that()
            .resideInAPackage("..dto.request..")
            .should()
            .haveSimpleNameEndingWith("Request");

    @ArchTest
    static final ArchRule RESPONSES_ARE_NAMED_DTO = classes()
            .that()
            .resideInAPackage("..dto.response..")
            .should()
            .haveSimpleNameEndingWith("Dto");

    @ArchTest
    static final ArchRule SERVICES_ARE_INTERFACES = classes()
            .that()
            .resideInAPackage("..service")
            .should()
            .beInterfaces()
            .andShould()
            .haveSimpleNameStartingWith("I")
            .andShould()
            .haveSimpleNameEndingWith("Service");

    @ArchTest
    static final ArchRule SERVICE_IMPLEMENTATIONS_IMPLEMENT_SERVICES = classes()
            .that()
            .resideInAPackage("..service.implementation..")
            .and()
            .areTopLevelClasses()
            .should()
            .beAnnotatedWith(Service.class)
            .andShould()
            .implement(resideInAPackage("..service"));

    @ArchTest
    static final ArchRule REST_CONTROLLERS_ARE_IN_CONTROLLER = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .resideInAPackage("..controller..")
            .andShould()
            .haveSimpleNameEndingWith("Controller");

    /** Spring Data derived queries use {@code _} as a property path separator, so repositories are left out. */
    @ArchTest
    static final ArchRule METHOD_NAMES_ARE_CAMEL_CASE = methods()
            .that()
            .areDeclaredInClassesThat()
            .resideOutsideOfPackage("..repository..")
            .and()
            .doNotHaveModifier(JavaModifier.SYNTHETIC)
            .should()
            .haveNameMatching("[a-z][a-zA-Z0-9]*");

    @ArchTest
    static final ArchRule NO_FIELD_INJECTION = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    @ArchTest
    static final ArchRule NO_STANDARD_STREAMS = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule NO_JAVA_UTIL_LOGGING = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    static final ArchRule NO_GENERIC_EXCEPTIONS = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    @ArchTest
    static final ArchRule NO_IS_USER_IN_ROLE = noClasses()
            .should()
            .callMethodWhere(target(name("isUserInRole"))
                    .and(target(owner(assignableTo(HttpServletRequest.class)))))
            .because("controllers read roles via @AuthenticationPrincipal(expression = \"isAdmin()\")");

    @ArchTest
    static final ArchRule TIME_COMES_FROM_CLOCK = noClasses()
            .should()
            .callMethodWhere(target(name("now"))
                    .and(target(owner(resideInAPackage("java.time"))))
                    .and(target(rawParameterTypes(new Class<?>[0]))))
            .because("the current time comes from the injected Clock");

    @ArchTest
    static final ArchRule NO_MAP_OF = noClasses()
            .should()
            .callMethodWhere(target(name("of"))
                    .and(target(owner(type(Map.class)))))
            .because("map literals use Map.ofEntries(Map.entry(k, v), ...)");

}
