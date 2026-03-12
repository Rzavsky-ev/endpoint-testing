package api.utils;

import api.exceptions.UtilityClassException;
import io.qameta.allure.Allure;
import io.restassured.response.Response;

import static api.specs.ResponseSpec.*;
import static api.utils.Constants.*;
import static org.hamcrest.Matchers.*;

/**
 * Утилитный класс для проверки ответов API.
 * <p>
 * Содержит методы верификации для различных сценариев:
 * успешные операции, ошибки валидации, ошибки авторизации,
 * ошибки внешних сервисов и многопользовательские сценарии.
 * Каждый метод выполняет проверку через ResponseSpec и добавляет
 * подробный отчет в Allure.
 */
public class ResponseVerifier {

    private ResponseVerifier() {
        throw new UtilityClassException(getClass());
    }

    /**
     * Проверяет успешный ответ от сервера.
     *
     * @param response объект ответа от сервера
     */
    public static void verifySuccess(Response response) {
        response.then().spec(forSuccess());
        reportActionSuccess(response);
    }

    /**
     * Проверяет ошибку неверного формата токена.
     *
     * @param response объект ответа от сервера
     */
    public static void verifyInvalidTokenError(Response response) {
        response.then()
                .spec(forError())
                .body(MESSAGE_PARAM, equalTo(INVALID_TOKEN_ERROR));
        reportError(response);
    }

    /**
     * Проверяет ошибку "токен не найден в системе".
     *
     * @param response объект ответа от сервера
     * @param token    токен, который не был найден
     */
    public static void verifyTokenNotFoundError(Response response, String token) {
        response.then()
                .spec(forError())
                .body(MESSAGE_PARAM, equalTo(String.format(TOKEN_NOT_FOUND_ERROR, token)));

        reportError(response);
    }

    /**
     * Проверяет ошибку неверного API ключа.
     *
     * @param response объект ответа от сервера
     */
    public static void verifyInvalidApiKeyError(Response response) {
        response.then()
                .spec(forError())
                .body(MESSAGE_PARAM, equalTo(INVALID_API_KEY_ERROR));

        reportError(response);
    }

    /**
     * Проверяет ошибку отсутствия или неверного значения параметра action.
     *
     * @param response объект ответа от сервера
     */
    public static void verifyMissingActionError(Response response) {
        response.then()
                .spec(forError())
                .body(MESSAGE_PARAM, equalTo(INVALID_ACTION_ERROR));

        reportError(response);
    }

    /**
     * Проверяет ошибку повторного использования токена.
     *
     * @param response объект ответа от сервера
     * @param token    токен, который уже используется
     */
    public static void verifyTokenAlreadyExistsError(Response response, String token) {
        response.then()
                .spec(forError())
                .body(MESSAGE_PARAM, equalTo(String.format(TOKEN_ALREADY_EXISTS_ERROR, token)));

        reportError(response);
    }

    /**
     * Проверяет обработку сбоя внешнего сервиса.
     *
     * @param response объект ответа от сервера
     */
    public static void verifyExternalServiceError(Response response) {
        response.then()
                .spec(forError())
                .body(MESSAGE_PARAM, not(blankOrNullString()));
        reportError(response);
    }

    /**
     * Добавляет в Allure отчет информацию об успешном действии.
     *
     * @param response объект ответа от сервера
     */
    private static void reportActionSuccess(Response response) {
        int statusCode = response.getStatusCode();
        String result = response.jsonPath().getString(RESULT_PARAM);

        Allure.addAttachment("Анализ успешного действия",
                String.format("""
                                ПРОВЕРКА УСПЕШНОГО ДЕЙСТВИЯ
                                
                                ОЖИДАЛОСЬ:
                                • result: "OK"
                                
                                ПОЛУЧЕНО:
                                • HTTP статус: %d %s
                                • result: "%s"
                                
                                РЕЗУЛЬТАТ ПРОВЕРКИ:
                                ✓ Действие успешно
                                """,
                        statusCode,
                        getStatusText(statusCode),
                        result));
    }

    /**
     * Добавляет в Allure отчет информацию об ошибке.
     *
     * @param response объект ответа от сервера
     */
    private static void reportError(Response response) {
        int statusCode = response.getStatusCode();
        String actualResult = response.jsonPath().getString(RESULT_PARAM);
        String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

        Allure.addAttachment("Анализ ошибки",
                String.format("""                                
                                ОЖИДАЛОСЬ:
                                • "result": "ERROR",
                                • "message": "%s"
                                
                                ПОЛУЧЕНО:
                                • HTTP Статус: %d %s
                                • Result: "%s"
                                • Message: "%s"
                                """,
                        actualMessage,
                        statusCode,
                        getStatusText(statusCode),
                        actualResult,
                        actualMessage != null ? actualMessage : NO_MESSAGE));
    }

    /**
     * Преобразует HTTP статус код в текстовое описание.
     *
     * @param statusCode HTTP статус код
     * @return понятное описание статуса
     */
    private static String getStatusText(int statusCode) {
        return switch (statusCode) {
            case 200 -> RESULT_OK;
            case 400 -> BAD_REQUEST_MESSAGE;
            case 401 -> UNAUTHORIZED_MESSAGE;
            case 403 -> FORBIDDEN_MESSAGE;
            case 404 -> NOT_FOUND_MESSAGE;
            case 409 -> CONFLICT_MESSAGE;
            case 500 -> INTERNAL_MESSAGE;
            default -> UNKNOWN_MESSAGE;
        };
    }
}