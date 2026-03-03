package api.utils;

import api.exceptions.UtilityClassException;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static api.specs.ResponseSpec.*;
import static api.utils.AllureReporter.addTestData;
import static api.utils.AllureReporter.getStatusText;
import static api.utils.Constants.*;
import static org.hamcrest.Matchers.*;

/**
 * Утилитный класс для проверки ответов API.
 * <p>
 * Содержит методы верификации для различных сценариев:
 * успешные операции, ошибки валидации, ошибки авторизации,
 * ошибки внешних сервисов и многопользовательские сценарии.
 * Каждый метод добавляет подробный отчет в Allure.
 */
public class ResponseVerifier {

    private ResponseVerifier() {
        throw new UtilityClassException(getClass());
    }

    /**
     * Проверяет успешный вход в систему (LOGIN).
     * Ожидает: статус 200.
     *
     * @param response объект ответа от сервера
     */
    public static void verifyLoginSuccess(Response response) {
        int statusCode = response.getStatusCode();
        String result = response.jsonPath().getString(RESULT_PARAM);

        addTestData("Результат входа в систему",
                String.format("""
                                ПРОВЕРКА УСПЕШНОГО ВХОДА В СИСТЕМУ
                                
                                ОЖИДАЛОСЬ:
                                • Статус: 200 OK (успешно)
                                • Результат: "OK" (вход выполнен)
                                
                                ПОЛУЧЕНО:
                                • Статус: %d %s
                                • Результат: %s
                                
                                ИТОГ ПРОВЕРКИ:
                                • Вход в систему: %s
                                """,
                        statusCode,
                        getStatusText(statusCode),
                        result != null ? result : "отсутствует",
                        statusCode == HTTP_OK ? "Да" : "Нет"));

        response.then().spec(forSuccess());
    }

    /**
     * Проверяет успешное выполнение действия (ACTION).
     * Ожидает: статус 200.
     *
     * @param response объект ответа от сервера
     * @param stepName название шага для отчета
     */
    public static void verifyActionSuccess(Response response, String stepName) {
        int statusCode = response.getStatusCode();
        String result = response.jsonPath().getString(RESULT_PARAM);

        addTestData(stepName,
                String.format("""
                                %s
                                
                                ОЖИДАЛОСЬ:
                                • Статус: 200 OK (успешно)
                                • Результат: "OK" (действие выполнено)
                                
                                ПОЛУЧЕНО:
                                • Статус: %d %s
                                • Результат: %s
                                """,
                        stepName,
                        statusCode,
                        getStatusText(statusCode),
                        result != null ? result : "отсутствует"));

        response.then().spec(forSuccess());
    }

    /**
     * Проверяет успешный выход из системы (LOGOUT).
     * Ожидает: статус 200.
     *
     * @param response объект ответа от сервера
     */
    public static void verifyLogoutSuccess(Response response) {
        int statusCode = response.getStatusCode();
        String result = response.jsonPath().getString(RESULT_PARAM);

        addTestData("Результат выхода из системы",
                String.format("""
                                ПРОВЕРКА УСПЕШНОГО ВЫХОДА ИЗ СИСТЕМЫ
                                
                                ОЖИДАЛОСЬ:
                                • Статус: 200 OK (успешно)
                                • Результат: "OK" (выход выполнен)
                                
                                ПОЛУЧЕНО:
                                • Статус: %d %s
                                • Результат: %s
                                
                                ИТОГ ПРОВЕРКИ:
                                • Выход из системы: %s
                                """,
                        statusCode,
                        getStatusText(statusCode),
                        result != null ? result : "отсутствует",
                        statusCode == HTTP_OK ? "Да" : "Нет"));

        response.then().spec(forSuccess());
    }

    /**
     * Проверяет ошибку неверного формата токена.
     * Ожидает: статус 400, сообщение INVALID_TOKEN_ERROR.
     *
     * @param response объект ответа от сервера
     */
    public static void verifyInvalidTokenError(Response response) {
        int statusCode = response.getStatusCode();
        String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

        addTestData("Анализ ошибки: неверный формат токена",
                String.format("""
                                ПРОВЕРКА ОШИБКИ ФОРМАТА ТОКЕНА
                                
                                ОЖИДАЛОСЬ:
                                • Статус: 400 Bad Request (некорректный запрос)
                                • Сообщение: "Неверный формат токена"
                                
                                ПОЛУЧЕНО:
                                • Статус: %d %s
                                • Сообщение: "%s"
                                
                                ИТОГ ПРОВЕРКИ:
                                • Формат токена: %s
                                • Сообщение об ошибке: %s
                                """,
                        statusCode,
                        getStatusText(statusCode),
                        actualMessage != null ? actualMessage : "отсутствует",
                        statusCode == HTTP_BAD_REQUEST ? "Да" : "Нет",
                        INVALID_TOKEN_ERROR.equals(actualMessage) ? "Да" : "Нет"));

        response.then()
                .spec(forValidationError())
                .body(MESSAGE_PARAM, equalTo(INVALID_TOKEN_ERROR));
    }

    /**
     * Проверяет ошибку "токен не найден в системе".
     * Ожидает: статус 403, сообщение TOKEN_NOT_FOUND_ERROR.
     *
     * @param response объект ответа от сервера
     * @param token    токен, который не был найден
     */
    public static void verifyTokenNotFoundError(Response response, String token) {
        int statusCode = response.getStatusCode();
        String expectedMessage = String.format(TOKEN_NOT_FOUND_ERROR, token);
        String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

        addTestData("Анализ ошибки: токен не найден в системе",
                String.format("""
                                ПРОВЕРКА ОШИБКИ "ТОКЕН НЕ НАЙДЕН"
                                
                                ОЖИДАЛОСЬ:
                                • Статус: 403 Forbidden (доступ запрещен)
                                • Сообщение: "%s"
                                
                                ПОЛУЧЕНО:
                                • Статус: %d %s
                                • Сообщение: "%s"
                                
                                ИТОГ ПРОВЕРКИ:
                                • Статус ответа: %s
                                • Сообщение об ошибке: %s
                                • Токен в системе: %s
                                """,
                        expectedMessage,
                        statusCode,
                        getStatusText(statusCode),
                        actualMessage != null ? actualMessage : "отсутствует",
                        statusCode == HTTP_FORBIDDEN ? "Да" : "Нет",
                        expectedMessage.equals(actualMessage) ? "Да" : "Нет",
                        statusCode == HTTP_FORBIDDEN ? "не найден" : "требуется анализ"));

        response.then()
                .spec(forTokenNotFoundError(token));
    }

    /**
     * Проверяет ошибку неверного API ключа.
     * Ожидает: статус 401, сообщение INVALID_API_KEY_ERROR.
     *
     * @param response объект ответа от сервера
     */
    public static void verifyInvalidApiKeyError(Response response) {
        int statusCode = response.getStatusCode();
        String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

        addTestData("Анализ ошибки: неверный ключ доступа",
                String.format("""
                                ПРОВЕРКА ОШИБКИ КЛЮЧА ДОСТУПА
                                
                                ОЖИДАЛОСЬ:
                                • Статус: 401 Unauthorized (не авторизован)
                                • Сообщение: "Неверный API ключ"
                                
                                ПОЛУЧЕНО:
                                • Статус: %d %s
                                • Сообщение: "%s"
                                
                                ИТОГ ПРОВЕРКИ:
                                • Статус ответа: %s
                                • Сообщение об ошибке: %s
                                • Ключ доступа: %s
                                """,
                        statusCode,
                        getStatusText(statusCode),
                        actualMessage != null ? actualMessage : "отсутствует",
                        statusCode == HTTP_UNAUTHORIZED ? "Да" : "Нет",
                        INVALID_API_KEY_ERROR.equals(actualMessage) ? "Да" : "Нет",
                        statusCode == HTTP_UNAUTHORIZED ? "отклонен" : "требуется анализ"));

        response.then()
                .spec(forInvalidApiKeyError());
    }

    /**
     * Проверяет ошибку отсутствия параметра action.
     * Ожидает: статус 400, сообщение INVALID_ACTION_ERROR.
     *
     * @param response объект ответа от сервера
     */
    public static void verifyMissingActionError(Response response) {
        int statusCode = response.getStatusCode();
        String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

        addTestData("Анализ ошибки: не указано действие",
                String.format("""
                                ПРОВЕРКА ОШИБКИ "НЕ УКАЗАНО ДЕЙСТВИЕ"
                                
                                ОЖИДАЛОСЬ:
                                • Статус: 400 Bad Request (некорректный запрос)
                                • Сообщение: "Неверное значение параметра action"
                                
                                ПОЛУЧЕНО:
                                • Статус: %d %s
                                • Сообщение: "%s"
                                
                                ИТОГ ПРОВЕРКИ:
                                • Статус ответа: %s
                                • Сообщение об ошибке: %s
                                • Параметр action: %s
                                """,
                        statusCode,
                        getStatusText(statusCode),
                        actualMessage != null ? actualMessage : "отсутствует",
                        statusCode == HTTP_BAD_REQUEST ? "Да" : "Нет",
                        INVALID_ACTION_ERROR.equals(actualMessage) ? "Да" : "Нет",
                        statusCode == HTTP_BAD_REQUEST ? "отсутствует" : "требуется анализ"));

        response.then()
                .spec(forError(HTTP_BAD_REQUEST))
                .body(MESSAGE_PARAM, equalTo(INVALID_ACTION_ERROR));
    }

    /**
     * Проверяет ошибку повторного использования токена.
     * Ожидает: статус 409, сообщение TOKEN_ALREADY_EXISTS_ERROR.
     *
     * @param response объект ответа от сервера
     * @param token    токен, который уже используется
     */
    public static void verifyTokenAlreadyExistsError(Response response, String token) {
        int statusCode = response.getStatusCode();
        String expectedMessage = String.format(TOKEN_ALREADY_EXISTS_ERROR, token);
        String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

        addTestData("Анализ ошибки: токен уже используется",
                String.format("""
                                ПРОВЕРКА ОШИБКИ "ТОКЕН УЖЕ ИСПОЛЬЗУЕТСЯ"
                                
                                ОЖИДАЛОСЬ:
                                • Статус: 409 Conflict (конфликт)
                                • Сообщение: "%s"
                                
                                ПОЛУЧЕНО:
                                • Статус: %d %s
                                • Сообщение: "%s"
                                
                                ИТОГ ПРОВЕРКИ:
                                • Статус ответа: %s
                                • Сообщение об ошибке: %s
                                • Повторный вход: %s
                                """,
                        expectedMessage,
                        statusCode,
                        getStatusText(statusCode),
                        actualMessage != null ? actualMessage : "отсутствует",
                        statusCode == HTTP_CONFLICT ? "Да" : "Нет",
                        expectedMessage.equals(actualMessage) ? "Да" : "Нет",
                        statusCode == HTTP_CONFLICT ? "отклонен" : "должен быть отклонен"));

        response.then()
                .spec(forError(HTTP_CONFLICT))
                .body(MESSAGE_PARAM, equalTo(expectedMessage));
    }

    /**
     * Проверяет обработку сбоя внешнего сервиса.
     * Ожидает: статус 500.
     *
     * @param response           объект ответа от сервера
     * @param expectedStatusCode код ошибки, возвращенный внешним сервисом (для информации)
     */
    public static void verifyExternalServiceError(Response response, int expectedStatusCode) {
        int actualStatusCode = response.getStatusCode();
        String actualResult = response.jsonPath().getString(RESULT_PARAM);
        String actualMessage = response.jsonPath().getString(MESSAGE_PARAM);

        addTestData("Анализ ответа при сбое внешнего сервиса",
                String.format("""
                                ПРОВЕРКА ОБРАБОТКИ СБОЯ ВНЕШНЕГО СЕРВИСА
                                
                                ЧТО ПРОВЕРЯЕТСЯ:
                                Как система обрабатывает ситуацию, когда внешний сервис
                                проверки токенов временно недоступен или возвращает ошибку
                                
                                ПОЛУЧЕНО ОТ СИСТЕМЫ:
                                • Статус ответа: %d %s
                                • Результат: %s
                                • Сообщение: %s
                                
                                РЕЗУЛЬТАТ ПРОВЕРКИ:
                                • Формат ответа (JSON): %s
                                • Статус операции (ERROR): %s
                                • Наличие описания ошибки: %s
                                
                                ПРИМЕЧАНИЕ:
                                В текущей реализации приложение всегда возвращает 500 Internal Server Error
                                независимо от кода ошибки внешнего сервиса (403, 404 или 500).
                                """,
                        actualStatusCode,
                        getStatusText(actualStatusCode),
                        actualResult != null ? actualResult : "отсутствует",
                        actualMessage != null ? "\"" + actualMessage + "\"" : "отсутствует",
                        response.getContentType() != null && response.getContentType().contains(APPLICATION_JSON)
                                ? "JSON" : "не JSON",
                        RESULT_ERROR.equals(actualResult) ? ERROR_MESSAGE : "ожидалось " + ERROR_MESSAGE,
                        actualMessage != null && !actualMessage.trim().isEmpty() ? "есть" : "отсутствует"));

        response.then()
                .contentType(ContentType.JSON)
                .body(RESULT_PARAM, equalTo(RESULT_ERROR))
                .body(MESSAGE_PARAM, not(blankOrNullString()));
    }
}