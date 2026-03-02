package api.tests;

import api.base.BaseTest;
import api.utils.TokenGenerator;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static api.specs.RequestSpec.*;
import static api.utils.AllureReporter.addTestData;
import static api.utils.Constants.*;
import static api.utils.ResponseVerifier.*;
import static api.utils.WireMockStubBuilder.mockAuthError;
import static api.utils.WireMockStubBuilder.mockAuthSuccess;
import static io.restassured.RestAssured.given;

@Epic("Тестирование веб-сервиса")
@Feature("Функционал LOGIN")
@DisplayName("Тесты для входа в систему (LOGIN)")
public class LoginTest extends BaseTest {

    @Test
    @Story("Успешные сценарии")
    @DisplayName("Вход в систему с валидным токеном")
    @Description("""
            Проверяет успешный вход в систему:
            - Генерируется валидный токен (32 символа, только цифры и буквы A-F)
            - Внешний сервис подтверждает валидность токена
            - Система возвращает сообщение об успехе: {"result": "OK"}
            """)
    @Tag(SMOKE)
    void successfulLoginWithValidToken() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s
                                        Длина: %d символов
                                        Ожидаемый результат: успешный вход, ответ: OK
                                        """,
                                token,
                                token.length())));

        Allure.step("Имитация работы внешнего сервиса", () -> {
            mockAuthSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервис проверки токенов настроен на успешный ответ");
        });

        Allure.step("Выполнение запроса на вход в систему", () -> {
            Response response = given()
                    .spec(forValidApiKey(token, ACTION_LOGIN))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается успешная аутентификация", () ->
                    verifyLoginSuccess(response));
        });
    }

    @ParameterizedTest(name = ": {0}")
    @MethodSource("api.utils.TokenGenerator#invalidTokenDataProvider")
    @Story("Ошибки валидации")
    @Tag(REGRESSION)
    @DisplayName("Вход в систему с некорректным токеном")
    @Description("""
            Проверяет обработку некорректных токенов при входе:
            - Токен не соответствует требуемому формату (должен быть 32 символа, только цифры и буквы A-F)
            - Система отклоняет запрос с сообщением об ошибке
            - Ожидается ответ: {"result": "ERROR", "message": "Неверный формат токена"}
            """)
    void loginWithInvalidToken(String testCase, String token, String description) {
        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий", description));

        Allure.step("Выполнение запроса на вход с некорректным токеном", () -> {
            Response response = given()
                    .spec(forValidApiKey(token, ACTION_LOGIN))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'неверный формат токена'", () ->
                    verifyInvalidTokenError(response));
        });
    }

    @ParameterizedTest(name = "Код ошибки: {0}")
    @ValueSource(ints = {HTTP_FORBIDDEN, HTTP_NOT_FOUND, HTTP_INTERNAL_ERROR})
    @Story("Ошибки внешнего сервиса")
    @DisplayName("Вход в систему при сбое внешнего сервиса")
    @Description("""
            Проверяет поведение системы при сбоях внешнего сервиса аутентификации:
            - Внешний сервис возвращает ошибку (доступ запрещен, ресурс не найден, внутренняя ошибка)
            - Проверяется, как система обрабатывает сбой внешнего сервиса
            
            ВАЖНО: В текущей реализации система всегда возвращает 500 Internal Server Error
            независимо от типа сбоя внешнего сервиса. Это поведение требует уточнения.
            """)
    @Tag(NEEDS_CLARIFICATION)
    @Tag(REGRESSION)
    void loginWhenExternalServiceReturnsError(int statusCode) {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Тип ошибки внешнего сервиса: %d
                                        Токен: %s (валидный)
                                        Ожидается: ответ с описанием ошибки
                                        """,
                                statusCode, token)));

        Allure.step("Имитация сбоя внешнего сервиса", () -> {
            mockAuthError(token, statusCode);
            addTestData("Настройка тестового окружения",
                    String.format("Сервис проверки токенов настроен на возврат ошибки с кодом %d", statusCode));
        });

        Allure.step("Выполнение запроса на вход при сбое внешнего сервиса", () -> {
            Response response = given()
                    .spec(forValidApiKey(token, ACTION_LOGIN))
                    .when()
                    .post(ENDPOINT);

            Allure.step(String.format("Проверка ответа: ожидается ошибка, вызванная сбоем внешнего сервиса (код %d)",
                    statusCode), () ->
                    verifyExternalServiceError(response, statusCode));
        });
    }

    @Test
    @Story("Ошибки аутентификации")
    @DisplayName("Вход в систему без ключа доступа")
    @Description("""
            Проверяет обработку запроса на вход без обязательного ключа доступа (API ключа):
            - Запрос отправляется без заголовка X-Api-Key
            - Система отклоняет запрос с ошибкой авторизации
            - Ожидается ответ с кодом 401 Unauthorized
            """)
    @Tag(SMOKE)
    void loginWithoutApiKey() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        Ключ доступа: отсутствует
                                        Ожидаемый результат: ошибка доступа 401 Unauthorized
                                        """,
                                token)));

        Allure.step("Выполнение запроса на вход без ключа доступа", () -> {
            Response response = given()
                    .spec(forEmptyApiKey(token, ACTION_LOGIN))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'отсутствует ключ доступа'", () ->
                    verifyInvalidApiKeyError(response));
        });
    }

    @Test
    @Story("Ошибки аутентификации")
    @DisplayName("Вход в систему с неверным ключом доступа")
    @Description("""
            Проверяет обработку запроса на вход с некорректным ключом доступа:
            - Запрос отправляется с недействительным значением X-Api-Key
            - Система отклоняет запрос с ошибкой авторизации
            - Ожидается ответ с кодом 401 Unauthorized
            """)
    @Tag(REGRESSION)
    void loginWithInvalidApiKey() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        Ключ доступа: 'wrong-key' (недействительный)
                                        Ожидаемый результат: ошибка доступа 401 Unauthorized
                                        """,
                                token)));

        Allure.step("Выполнение запроса на вход с некорректным ключом доступа", () -> {
            Response response = given()
                    .spec(forWrongApiKey(token, ACTION_LOGIN))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'неверный ключ доступа'", () ->
                    verifyInvalidApiKeyError(response));
        });
    }

    @Test
    @Story("Поведение при повторных операциях")
    @DisplayName("Повторный вход в систему с тем же токеном")
    @Description("""
            Проверяет поведение системы при повторном входе с уже использованным токеном:
            - Первый запрос на вход выполняется успешно
            - Второй запрос с тем же токеном должен быть отклонен
            - Ожидается ошибка 409 Conflict, так как токен уже активен
            """)
    @Tag(REGRESSION)
    void loginTwiceWithSameToken() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s
                                        Сценарий теста:
                                        1. Первый вход в систему (ожидается успех)
                                        2. Повторный вход с тем же токеном (ожидается ошибка)
                                        Ожидаемый результат: ошибка 409 Conflict
                                        """,
                                token)));

        Allure.step("Имитация работы внешнего сервиса", () -> {
            mockAuthSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервис проверки токенов настроен на успешный ответ");
        });

        Allure.step("Первый вход в систему", () -> {
            Response firstResponse = given()
                    .spec(forValidApiKey(token, ACTION_LOGIN))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка первого ответа: ожидается успешный вход", () ->
                    verifyLoginSuccess(firstResponse));
        });

        Allure.step("Попытка повторного входа с тем же токеном", () -> {
            Response secondResponse = given()
                    .spec(forValidApiKey(token, ACTION_LOGIN))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'токен уже используется'", () ->
                    verifyTokenAlreadyExistsError(secondResponse, token));
        });
    }

    @Test
    @Story("Ошибки валидации")
    @DisplayName("Вход в систему без указания действия")
    @Description("""
            Проверяет обработку запроса на вход без обязательного параметра action:
            - Запрос отправляется без указания типа действия
            - Система не может определить, какое действие требуется выполнить
            - Ожидается ошибка валидации с кодом 400
            """)
    @Tag(REGRESSION)
    void loginWithoutActionParameter() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        Параметр action: не указан
                                        Ожидаемый результат: ошибка валидации 400
                                        """,
                                token)));

        Allure.step("Выполнение запроса на вход без указания действия", () -> {
            Response response = given()
                    .spec(forValidApiKey(token, ""))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'отсутствует обязательный параметр action'", () ->
                    verifyMissingActionError(response));
        });
    }
}