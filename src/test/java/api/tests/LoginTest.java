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
import static api.utils.ActionSteps.performLogin;
import static api.utils.AllureReporter.addTestData;
import static api.utils.Constants.*;
import static api.utils.ResponseVerifier.*;
import static api.utils.WireMockStubBuilder.*;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;

@Epic("Тестирование веб-сервиса")
@Feature("Функционал LOGIN")
@DisplayName("Тесты для входа в систему - LOGIN")
public class LoginTest extends BaseTest {

    @Test
    @Story("Успешные сценарии")
    @DisplayName("Вход в систему с валидным токеном")
    @Description("""
            Проверяет успешный вход в систему:
            - Генерируется валидный токен (32 символа, только цифры и буквы A-F)
            - Внешний сервис подтверждает валидность токена
            - Система возвращает сообщение об успехе: {"result":"OK"}
            """)
    @Tag(SMOKE)
    void successfulLoginWithValidToken() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s
                                        Длина: %d символов
                                        Ожидаемый результат: успешный вход
                                        """,
                                token,
                                token.length())));

        Allure.step("Имитация работы внешнего сервиса", () -> {
            mockAuthSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервис проверки токенов настроен на успешный ответ");
        });

        Allure.step("Выполнение запроса на вход в систему", () -> {
            Response response = performLogin(token);

            Allure.step("Проверка ответа: ожидается успешная аутентификация", () ->
                    verifySuccess(response));
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
            - Ожидается ответ: {"result": "ERROR", "message":"token: должно соответствовать \\"^[0-9A-F]{32}$\\""}
            """)
    void loginWithInvalidToken(String testCase, String token, String description) {
        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий", description));

        Allure.step("Выполнение запроса на вход с некорректным токеном", () -> {
            Response response = performLogin(token);

            Allure.step("Проверка ответа: ожидается ошибка 'token: должно соответствовать \"^[0-9A-F]{32}$\"'",
                    () -> verifyInvalidTokenError(response));
            addTestData("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Система отклонила запрос с некорректным токеном");
        });

        Allure.step("Проверка, что внешний сервис не вызывался", () -> {
            wireMockServer.verify(0, postRequestedFor(urlEqualTo(MOCK_AUTH)));
            addTestData("Проверка вызовов внешнего сервиса",
                    "✓ Внешний сервис аутентификации НЕ вызывался (корректно)");
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
            - Ожидается ответ: {"result": "ERROR", "message":"Internal Server Error"}

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
                                        Ожидается: ошибка с описанием причины
                                        """,
                                statusCode, token)));

        Allure.step("Имитация сбоя внешнего сервиса", () -> {
            mockAuthError(token, statusCode);
            addTestData("Настройка тестового окружения",
                    String.format("Сервис проверки токенов настроен на возврат ошибки с кодом %d", statusCode));
        });

        Allure.step("Выполнение запроса на вход при сбое внешнего сервиса", () -> {
            Response response = performLogin(token);

            Allure.step("Проверка ответа: ожидается ошибка 'Internal Server Error'", () ->
                    verifyExternalServiceError(response));
            addTestData("ИТОГ ПРОВЕРКИ:",
                    """
                            ✓ Сбой внешнего сервиса обработан корректно
                            ✓ Сообщение об ошибке получено

                            ПРИМЕЧАНИЕ:
                            В текущей реализации приложение всегда возвращает 500 Internal Server Error
                            независимо от кода ошибки внешнего сервиса (403, 404 или 500).
                            """);
        });
    }


    @Test
    @Story("Ошибки аутентификации")
    @DisplayName("Вход в систему без ключа доступа")
    @Description("""
            Проверяет обработку запроса на вход без обязательного ключа доступа (API ключа):
            - Запрос отправляется без заголовка X-Api-Key
            - Система отклоняет запрос с ошибкой авторизации
            - Ожидается ответ: {"result": "ERROR", "message":"Missing or invalid API Key"}
            """)
    @Tag(SMOKE)
    void loginWithoutApiKey() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        Ключ доступа: отсутствует
                                        Ожидается: ошибка с описанием причины
                                        """,
                                token)));

        Allure.step("Выполнение запроса на вход без ключа доступа", () -> {
            Response response = given()
                    .spec(forEmptyApiKey(token, ACTION_LOGIN))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'Missing or invalid API Key'", () ->
                    verifyInvalidApiKeyError(response));

            addTestData("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Неверный API ключ отклонен");

        });
        Allure.step("Проверка, что внешний сервис не вызывался", () -> {
            wireMockServer.verify(0, postRequestedFor(urlEqualTo(MOCK_AUTH)));
            addTestData("Проверка вызовов внешнего сервиса",
                    "✓ Внешний сервис аутентификации НЕ вызывался (корректно)");
        });
    }

    @Test
    @Story("Ошибки аутентификации")
    @DisplayName("Вход в систему с неверным ключом доступа")
    @Description("""
            Проверяет обработку запроса на вход с некорректным ключом доступа:
            - Запрос отправляется с недействительным значением X-Api-Key
            - Система отклоняет запрос с ошибкой авторизации
            - Ожидается ответ: {"result": "ERROR", "message": "Missing or invalid API Key"}
            """)
    @Tag(REGRESSION)
    void loginWithInvalidApiKey() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        Ключ доступа: 'wrong-key' (недействительный)
                                        Ожидается: ошибка с описанием причины
                                        """,
                                token)));

        Allure.step("Выполнение запроса на вход с некорректным ключом доступа", () -> {
            Response response = given()
                    .spec(forWrongApiKey(token, ACTION_LOGIN))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'Missing or invalid API Key'", () ->
                    verifyInvalidApiKeyError(response));

            addTestData("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Неверный API ключ отклонен");
        });

        Allure.step("Проверка, что внешний сервис не вызывался", () -> {
            wireMockServer.verify(0, postRequestedFor(urlEqualTo(MOCK_AUTH)));
            addTestData("Проверка вызовов внешнего сервиса",
                    "✓ Внешний сервис аутентификации НЕ вызывался (корректно)");
        });
    }

    @Test
    @Story("Поведение при повторных операциях")
    @DisplayName("Повторный вход в систему с тем же токеном")
    @Description("""
            Проверяет поведение системы при повторном входе с уже использованным токеном:
            - Первый запрос на вход выполняется успешно
            - Второй запрос с тем же токеном должен быть отклонен
            - Ожидается ответ: {"result": "ERROR", "message": "Token '<token>' already exists"}
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
                                        Ожидается: ошибка с описанием причины
                                        """,
                                token)));

        Allure.step("Имитация работы внешнего сервиса", () -> {
            mockAuthSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервис проверки токенов настроен на успешный ответ");
        });

        Allure.step("Первый вход в систему", () -> {
            Response firstResponse = performLogin(token);

            Allure.step("Проверка первого ответа: ожидается успешный вход", () ->
                    verifySuccess(firstResponse));
        });

        Allure.step("Попытка повторного входа с тем же токеном", () -> {
            Response secondResponse = performLogin(token);

            Allure.step(String.format("Проверка ответа: ожидается ошибка 'Token %s already exists'", token), () ->
                    verifyTokenAlreadyExistsError(secondResponse, token));
            addTestData("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Повторный вход с тем же токеном отклонен");
        });
    }

    @Test
    @Story("Ошибки валидации")
    @DisplayName("Вход в систему без указания действия")
    @Description("""
            Проверяет обработку запроса на вход без обязательного параметра action:
            - Запрос отправляется без указания типа действия
            - Система не может определить, какое действие требуется выполнить
            - Ожидается ответ: {"result": "ERROR", "message":"action: invalid action 'null'. Allowed: LOGIN, LOGOUT"}
            """)
    @Tag(REGRESSION)
    void loginWithoutActionParameter() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        Параметр action: не указан
                                        Ожидается: ошибка с описанием причины
                                        """,
                                token)));

        Allure.step("Выполнение запроса на вход без указания действия", () -> {
            Response response = given()
                    .spec(forValidApiKey(token, ""))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'action: invalid action 'null'. Allowed: LOGIN, LOGOUT'"
                    , () -> verifyMissingActionError(response));
            addTestData("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Отсутствие параметра action обработано корректно");
        });

        Allure.step("Проверка, что внешний сервис не вызывался", () -> {
            wireMockServer.verify(0, postRequestedFor(urlEqualTo(MOCK_AUTH)));

            addTestData("Проверка вызовов внешнего сервиса",
                    "✓ Внешний сервис аутентификации НЕ вызывался (корректно)");
        });
    }
}