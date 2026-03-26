package pl.prawko.prawko_server.constants;

/**
 * API endpoint constants.
 *
 * <p>Centralized repository for all REST API base URLs and path segments. These constants
 * are used consistently across all controllers to ensure unified API structure and simplify
 * maintenance of endpoint definitions.
 *
 */
public final class ApiConstants {

    /**
     * Base URL for user management endpoints.
     *
     * @see pl.prawko.prawko_server.controller.UserController
     */
    public static final String USERS_BASE_URL = "/users";

    /**
     * Nested URL for user management endpoints.
     *
     * @see pl.prawko.prawko_server.controller.UserController
     */
    public static final String USERS_BASE_URL_ALL = "/users/**";

    /**
     * Base URL for question management endpoints.
     *
     * @see pl.prawko.prawko_server.controller.QuestionController
     */
    public static final String QUESTIONS_BASE_URL = "/questions";

    /**
     * Nested URL for question management endpoints.
     *
     * @see pl.prawko.prawko_server.controller.QuestionController
     */
    public static final String QUESTIONS_BASE_URL_ALL = "/questions/**";

    /**
     * Base URL for exam management endpoints.
     *
     * @see pl.prawko.prawko_server.controller.ExamController
     */
    public static final String EXAMS_BASE_URL = "/exams";

    /**
     * Nested URL for exam management endpoints.
     *
     * @see pl.prawko.prawko_server.controller.ExamController
     */
    public static final String EXAMS_BASE_URL_ALL = "/exams/**";

    /**
     * Base URL for authentication endpoints.
     *
     * @see pl.prawko.prawko_server.controller.AuthController
     */
    public static final String AUTH_BASE_URL = "/auth";

    /**
     * Path variable template for resource identification.
     * Used with any of the base URLs to identify specific resources by ID.
     *
     * <p>Example: {@code /users/{id}}, {@code /exams/{id}}
     */
    public static final String BY_ID = "/{id}";

    private ApiConstants() {
    }

}
