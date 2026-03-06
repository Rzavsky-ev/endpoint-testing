package api.utils;

import api.exceptions.UtilityClassException;

/**
 * Центральный класс для хранения всех констант, используемых в тестах.
 * <p>
 * Содержит настройки подключения, эндпоинты, HTTP статусы, сообщения об ошибках,
 * тестовые данные и другие постоянные значения. Все константы сгруппированы
 * по функциональному назначению для удобства использования и поддержки.
 */
public class Constants {
    // ==================== URL и порты ====================
    public static final String HOST = "localhost";
    public static final int PORT = 8080;
    public static final String BASE_URL = "http://" + HOST + ":" + PORT;
    public static final int WIREMOCK_PORT = 8888;

    // ==================== Эндпоинты ====================
    public static final String ENDPOINT = "/endpoint";
    public static final String MOCK_AUTH = "/auth";
    public static final String MOCK_DO_ACTION = "/doAction";

    // ==================== Заголовки ====================
    public static final String API_KEY_HEADER_NAME = "X-Api-Key";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String ACCEPT = "Accept";
    public static final String TEXT_PLAIN = "text/plain";

    // ==================== Параметры запросов ====================
    public static final String TOKEN_PARAM = "token";
    public static final String ACTION_PARAM = "action";
    public static final String MESSAGE_PARAM = "message";
    public static final String RESULT_PARAM = "result";

    // ==================== Значения заголовков ====================
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_URLENCODED = "application/x-www-form-urlencoded";

    // ==================== HTTP статусы ====================
    public static final int HTTP_OK = 200;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_INTERNAL_ERROR = 500;

    // ==================== Сообщения об ошибках ====================
    public static final String FORBIDDEN_MESSAGE = "Forbidden";
    public static final String NOT_FOUND_MESSAGE = "Not Found";
    public static final String BAD_REQUEST_MESSAGE = "Bad Request";
    public static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
    public static final String CONFLICT_MESSAGE = "Conflict";
    public static final String INTERNAL_MESSAGE = "Internal Server Error";
    public static final String UNKNOWN_MESSAGE = "Unknown";
    public static final String TOKEN_NOT_FOUND_ERROR = "Token '%s' not found";
    public static final String INVALID_TOKEN_ERROR =
            "token: должно соответствовать \"^[0-9A-F]{32}$\"";
    public static final String INVALID_API_KEY_ERROR = "Missing or invalid API Key";
    public static final String TOKEN_ALREADY_EXISTS_ERROR = "Token '%s' already exists";
    public static final String INVALID_ACTION_ERROR =
            "action: invalid action 'null'. Allowed: LOGIN, LOGOUT, ACTION";
    public static final String NO_MESSAGE = "отсутствует";

    // ==================== Actions ====================
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_ACTION = "ACTION";
    public static final String ACTION_LOGOUT = "LOGOUT";

    // ==================== Результаты ====================
    public static final String RESULT_OK = "OK";
    public static final String RESULT_ERROR = "ERROR";

    // ==================== Тестовые данные ====================
    public static final String VALID_API_KEY = "qazWSXedc";
    public static final String INVALID_API_KEY = "invalid_key";
    public static final String EMPTY_API_KEY = "";

    // ==================== Прочее ====================
    public static final int TOKEN_LENGTH = 32;

    // ==================== Теги ====================
    public static final String SMOKE = "smoke";
    public static final String REGRESSION = "regression";
    public static final String NEEDS_CLARIFICATION = "needs-clarification";

    // ==================== WireMock responses ====================
    public static final String AUTH_SUCCESS_BODY = "{\"status\":\"success\"}";
    public static final String AUTH_ERROR_BODY = "{\"error\":\"authentication failed\"}";
    public static final String ACTION_SUCCESS_BODY = "{\"action\":\"completed\"}";
    public static final String ACTION_ERROR_BODY = "{\"error\":\"action failed\"}";

    private Constants() {
        throw new UtilityClassException(getClass());
    }
}