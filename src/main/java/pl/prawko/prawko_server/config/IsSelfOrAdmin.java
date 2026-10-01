package pl.prawko.prawko_server.config;

import org.springframework.security.access.prepost.PreAuthorize;
import pl.prawko.prawko_server.model.AuthenticatedUser;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows invoking the annotated method only by an admin or by the user the request concerns.
 *
 * <p>Template for {@link PreAuthorize}, resolved thanks to
 * {@link org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults} bean.
 * Requires the principal to be an {@link AuthenticatedUser}.
 *
 * <p>Example: {@code @IsSelfOrAdmin(userId = "#dto.userId()")}
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN') or principal.isSelf({userId})")
public @interface IsSelfOrAdmin {

    /**
     * SpEL expression evaluating to the ID of the user being accessed, e.g. {@code "#id"}.
     *
     * @return the expression resolving the user's ID
     */
    String userId();

}
