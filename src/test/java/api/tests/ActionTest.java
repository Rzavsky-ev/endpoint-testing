package api.tests;

import api.base.BaseTest;
import api.utils.TokenGenerator;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static api.specs.RequestSpec.*;
import static api.utils.ActionSteps.*;
import static api.utils.Constants.*;
import static api.utils.ResponseVerifier.*;
import static api.utils.WireMockStubBuilder.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;

@Epic("Тестирование веб-сервиса")
@Feature("Функционал ACTION")
@DisplayName("Тесты для действия - ACTION")
public class ActionTest extends BaseTest {

    @Test
    @Story("Успешные сценарии")
    @DisplayName("Выполнение ACTION после успешного LOGIN")
    @Description("""
            Проверяет успешное выполнение действия после аутентификации:
            - Генерируется валидный токен (32 символа, только цифры и буквы A-F)
            - Выполняется успешный LOGIN (токен сохраняется)
            - Выполняется ACTION с тем же токеном
            - Система возвращает сообщение об успехе: {"result":"OK"}
            """)
    @Tag(SMOKE)
    void performActionAfterSuccessfulLogin() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                Allure.addAttachment("Тестовый сценарий",
                        String.format("""
                                        Токен: %s
                                        Длина: %d символов
                                        Сценарий:
                                        1. LOGIN с токеном (ожидается успех)
                                        2. ACTION с тем же токеном (ожидается успех)
                                        """,
                                token,
                                token.length())));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            mockDoActionSuccess(token);
            Allure.addAttachment("Настройка тестового окружения",
                    "Сервис аутентификации и сервис выполнения действий настроены на успешный ответ");
        });

        performSuccessfulLogin(token);
        performSuccessfulAction(token);
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("ACTION с невалидным токеном после успешного LOGIN")
    @Description("""
            Проверяет обработку ACTION с невалидным токеном после успешной аутентификации:
            - Выполняется успешный LOGIN с валидным токеном
            - Отправляется запрос ACTION с другим (невалидным) токеном
            - Ожидается ответ: {"result": "ERROR", "message":"token: должно соответствовать \\"^[0-9A-F]{32}$\\""}
            """)
    @Tag(REGRESSION)
    void performActionWithInvalidTokenAfterValidLogin() {
        String validToken = TokenGenerator.generateValidToken();
        String invalidToken = TokenGenerator.generateTokenOfRandomLength();

        Allure.step("Подготовка тестовых данных", () ->
                Allure.addAttachment("Тестовый сценарий",
                        String.format("""
                                        Токен 1 (валидный): %s
                                        Длина: %d символов
                                        Токен 2 (невалидный): %s
                                        Длина: %d символов (требуется: 32)
                                        Сценарий:
                                        1. LOGIN с валидным токеном (ожидается успех)
                                        2. ACTION с невалидным токеном (ожидается ошибка)
                                        Ожидается: ошибка с описанием причины
                                        """,
                                validToken,
                                validToken.length(),
                                invalidToken,
                                invalidToken.length())));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(validToken);
            mockDoActionSuccess(validToken);
            Allure.addAttachment("Настройка тестового окружения",
                    "Сервис аутентификации и сервис выполнения действий настроены на успешный ответ");
        });

        performSuccessfulLogin(validToken);

        Allure.step("Выполнение ACTION с невалидным токеном", () -> {
            Response actionResponse = performAction(invalidToken);

            Allure.step("Проверка ответа: ожидается ошибка 'token: должно соответствовать \"^[0-9A-F]{32}$\"'", () ->
                    verifyInvalidTokenError(actionResponse));
            Allure.addAttachment("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Система отклонила запрос с некорректным токеном");
        });

        Allure.step("Проверка, что внешний сервис не вызывался с невалидным токеном", () -> {
            wireMockServer.verify(0, postRequestedFor(urlEqualTo(MOCK_DO_ACTION))
                    .withRequestBody(containing("token=" + invalidToken)));
            Allure.addAttachment("Проверка вызовов внешнего сервиса",
                    "✓ Внешний сервис НЕ вызывался с невалидным токеном");
        });
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("ACTION без предварительного LOGIN")
    @Description("""
            Проверяет обработку ACTION без предварительной аутентификации:
            - Токен никогда не проходил LOGIN
            - Отправляется запрос ACTION
            - Ожидается ответ: {"result": "ERROR", "message":"Token '<token>' not found"}
            """)
    @Tag(REGRESSION)
    void performActionWithoutLogin() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                Allure.addAttachment("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        Длина: %d символов
                                        Сценарий: Прямой запрос ACTION без LOGIN
                                        Ожидается: ошибка с описанием причины
                                        """,
                                token,
                                token.length())));

        Allure.step("Выполнение запроса ACTION без предварительной аутентификации", () -> {
            Response response = performAction(token);

            Allure.step("Проверка ответа: ожидается ошибка 'Token not found'", () ->
                    verifyTokenNotFoundError(response, token));
            Allure.addAttachment("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Система отклонила запрос с токеном, не прошедшим LOGIN");
        });
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("ACTION после LOGOUT")
    @Description("""
            Проверяет обработку ACTION после завершения сессии:
            - Выполняется успешный LOGIN
            - Выполняется LOGOUT (токен удаляется)
            - Отправляется запрос ACTION
            - Ожидается ответ: {"result": "ERROR", "message":"Token '<token>' not found"}
            """)
    @Tag(REGRESSION)
    void performActionAfterLogout() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                Allure.addAttachment("Тестовый сценарий",
                        String.format("""
                                        Токен: %s
                                        Длина: %d символов
                                        Сценарий:
                                        1. LOGIN с токеном (ожидается успех)
                                        2. LOGOUT с токеном (ожидается успех)
                                        3. ACTION с тем же токеном (ожидается ошибка)
                                        Ожидается: ошибка с описанием причины
                                        """,
                                token,
                                token.length())));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            mockDoActionSuccess(token);
            Allure.addAttachment("Настройка тестового окружения",
                    "Сервисы настроены на успешные ответы");
        });

        performSuccessfulLogin(token);
        performSuccessfulLogout(token);

        Allure.step("Попытка выполнения ACTION после завершения сессии", () -> {
            Response actionResponse = performAction(token);

            Allure.step(String.format("Проверка ответа: ожидается ошибка 'Token %s not found'", token), () ->
                    verifyTokenNotFoundError(actionResponse, token));
            Allure.addAttachment("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Система отклонила запрос с несуществующим токеном");
        });
    }

    @ParameterizedTest(name = "Код ошибки: {0}")
    @ValueSource(ints = {HTTP_FORBIDDEN, HTTP_NOT_FOUND, HTTP_INTERNAL_ERROR})
    @Story("Ошибки внешнего сервиса")
    @DisplayName("ACTION при ошибках внешнего сервиса")
    @Description("""
            Проверяет обработку различных ошибок от внешнего сервиса /doAction:
            - Токен успешно аутентифицирован
            - Внешний сервис /doAction возвращает ошибку
            - Проверяется ответ приложения на внешнюю ошибку
            - Ожидается ответ: {"result": "ERROR", "message":"Internal Server Error"}
            
            ВАЖНО: В текущей реализации приложение всегда возвращает 500 Internal Server Error
            независимо от кода ошибки внешнего сервиса (403, 404 или 500).
            Это поведение требует уточнения у разработчиков.
            """)
    @Tag(NEEDS_CLARIFICATION)
    @Tag(REGRESSION)
    void performActionWhenExternalServiceReturnsError(int statusCode) {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                Allure.addAttachment("Тестовый сценарий",
                        String.format("""
                                        Код ошибки внешнего сервиса: %d
                                        Токен: %s (валидный)
                                        Сценарий:
                                        1. LOGIN с токеном (ожидается успех)
                                        2. ACTION при ошибке внешнего сервиса /doAction
                                        Ожидается: ошибка с описанием причины
                                        """,
                                statusCode, token)));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            mockDoActionError(token, statusCode);
            Allure.addAttachment("Настройка тестового окружения",
                    String.format("Сервис выполнения действий настроен на возврат ошибки с кодом %d", statusCode));
        });

        performSuccessfulLogin(token);

        Allure.step("Выполнение ACTION при сбое внешнего сервиса", () -> {
            Response actionResponse = performAction(token);

            Allure.step("Проверка ответа: ожидается ошибка 'Internal Server Error'", () ->
                    verifyExternalServiceError(actionResponse));
            Allure.addAttachment("РЕЗУЛЬТАТ ПРОВЕРКИ:",
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
    @Story("Многократные операции")
    @DisplayName("Несколько ACTION подряд с одним токеном")
    @Description("""
            Проверяет возможность выполнения нескольких ACTION подряд:
            - Токен успешно аутентифицирован
            - Выполняется несколько запросов ACTION подряд
            - Все запросы должны быть успешными
            - Ожидается ответ: {"result":"OK"} для каждого действия
            """)
    @Tag(REGRESSION)
    void performMultipleActionsWithSameToken() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                Allure.addAttachment("Тестовый сценарий",
                        String.format("""
                                        Токен: %s
                                        Длина: %d символов
                                        Сценарий:
                                        1. LOGIN с токеном (ожидается успех)
                                        2. ACTION №1 (ожидается успех)
                                        3. ACTION №2 (ожидается успех)
                                        4. ACTION №3 (ожидается успех)
                                        """,
                                token,
                                token.length())));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            mockDoActionSuccess(token);
            Allure.addAttachment("Настройка тестового окружения",
                    "Сервисы настроены на успешные ответы");
        });

        performSuccessfulLogin(token);

        Allure.step("Выполнение нескольких запросов ACTION подряд", () -> {
            for (int i = 1; i <= 3; i++) {
                Response response = performAction(token);
                verifySuccess(response);
                Allure.addAttachment(String.format("Результат ACTION №%d", i),
                        String.format("✓ Действие %d выполнено успешно", i));
            }
            Allure.addAttachment("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Все 3 действия ACTION выполнены успешно");
        });
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("ACTION без API ключа")
    @Description("""
            Проверяет обработку запроса ACTION без обязательного заголовка X-Api-Key:
            - Токен успешно аутентифицирован
            - Отправляется запрос ACTION без заголовка авторизации
            - Ожидается ответ: {"result": "ERROR", "message":"Missing or invalid API Key"}
            """)
    @Tag(REGRESSION)
    void performActionWithoutApiKey() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                Allure.addAttachment("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        API ключ: отсутствует
                                        Сценарий:
                                        1. LOGIN с токеном (ожидается успех)
                                        2. ACTION без API ключа
                                        Ожидается: ошибка с описанием причины
                                        """,
                                token)));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            mockDoActionSuccess(token);
            Allure.addAttachment("Настройка тестового окружения",
                    "Сервисы настроены на успешные ответы");
        });

        performSuccessfulLogin(token);

        Allure.step("Выполнение ACTION без ключа доступа", () -> {
            Response response = given()
                    .spec(forEmptyApiKey(token, ACTION_ACTION))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'Missing or invalid API Key'", () ->
                    verifyInvalidApiKeyError(response));
            Allure.addAttachment("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Неверный API ключ отклонен");
        });

        Allure.step("Проверка, что внешний сервис не вызывался", () -> {
            wireMockServer.verify(0, postRequestedFor(urlEqualTo(MOCK_DO_ACTION)));
            Allure.addAttachment("Проверка вызовов внешнего сервиса",
                    "✓ Внешний сервис НЕ вызывался с отсутствующим API ключом");
        });
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("ACTION с неверным API ключом")
    @Description("""
            Проверяет обработку запроса ACTION с неверным API ключом:
            - Токен успешно аутентифицирован
            - Отправляется запрос ACTION с некорректным X-Api-Key
            - Ожидается ответ: {"result": "ERROR", "message":"Missing or invalid API Key"}
            """)
    @Tag(REGRESSION)
    void performActionWithInvalidApiKey() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                Allure.addAttachment("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        API ключ: 'wrong-key' (недействительный)
                                        Сценарий:
                                        1. LOGIN с токеном (ожидается успех)
                                        2. ACTION с неверным API ключом
                                        Ожидается: ошибка с описанием причины
                                        """,
                                token)));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            mockDoActionSuccess(token);
            Allure.addAttachment("Настройка тестового окружения",
                    "Сервисы настроены на успешные ответы");
        });

        performSuccessfulLogin(token);

        Allure.step("Выполнение ACTION с некорректным ключом доступа", () -> {
            Response response = given()
                    .spec(forWrongApiKey(token, ACTION_ACTION))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'Missing or invalid API Key'", () ->
                    verifyInvalidApiKeyError(response));
            Allure.addAttachment("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Неверный API ключ отклонен");
        });

        Allure.step("Проверка, что внешний сервис не вызывался", () -> {
            wireMockServer.verify(0, postRequestedFor(urlEqualTo(MOCK_DO_ACTION)));
            Allure.addAttachment("Проверка вызовов внешнего сервиса",
                    "✓ Внешний сервис НЕ вызывался с неверным API ключом");
        });
    }

    @Test
    @Story("Ошибки валидации")
    @DisplayName("ACTION без параметра action")
    @Description("""
            Проверяет обработку запроса ACTION без параметра action:
            - Токен успешно аутентифицирован
            - Отправляется запрос без параметра action
            - Ожидается ответ: {"result": "ERROR",
            "message":"action: invalid action 'null'. Allowed: LOGIN, LOGOUT, ACTION"}
            """)
    @Tag(REGRESSION)
    void performActionWithoutActionParameter() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                Allure.addAttachment("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        Длина: %d символов
                                        Параметр action: не указан
                                        Сценарий:
                                        1. LOGIN с токеном (ожидается успех)
                                        2. ACTION без параметра action (ожидается ошибка)
                                        Ожидается: ошибка с описанием причины
                                        """,
                                token,
                                token.length())));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            Allure.addAttachment("Настройка тестового окружения",
                    "Сервис аутентификации настроен на успешный ответ");
        });

        performSuccessfulLogin(token);

        Allure.step("Попытка выполнения ACTION без указания параметра action", () -> {
            Response actionResponse = given()
                    .spec(forValidApiKey(token, ""))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа:" +
                    " ожидается ошибка 'action: invalid action 'null'. Allowed: LOGIN, LOGOUT, ACTION'", () ->
                    verifyMissingActionError(actionResponse));
            Allure.addAttachment("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Отсутствие параметра action обработано корректно");
        });

        Allure.step("Проверка, что внешний сервис не вызывался", () -> {
            wireMockServer.verify(0, postRequestedFor(urlEqualTo(MOCK_DO_ACTION)));
            Allure.addAttachment("Проверка вызовов внешнего сервиса",
                    "✓ Внешний сервис НЕ вызывался с отсутствующим параметром action");
        });
    }
}