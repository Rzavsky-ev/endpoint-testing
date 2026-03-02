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
import static api.utils.AllureReporter.addTestData;
import static api.utils.Constants.*;
import static api.utils.ResponseVerifier.*;
import static api.utils.WireMockStubBuilder.*;
import static io.restassured.RestAssured.given;

@Epic("Тестирование веб-сервиса")
@Feature("Функционал ACTION")
@DisplayName("Тесты для действия ACTION")
public class ActionTest extends BaseTest {

    @Test
    @Story("Успешные сценарии")
    @DisplayName("Выполнение ACTION после успешного LOGIN")
    @Description("""
            Проверяет успешное выполнение действия после аутентификации:
            - Генерируется валидный токен
            - Выполняется успешный LOGIN (токен сохраняется)
            - Выполняется ACTION с тем же токеном
            - Ожидается успешный ответ
            """)
    @Tag(SMOKE)
    void performActionAfterSuccessfulLogin() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
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
            addTestData("Настройка тестового окружения",
                    "Сервис аутентификации и сервис выполнения действий настроены на успешный ответ");
        });

        performSuccessfulLogin(token);
        performSuccessfulAction(token, "Выполнение запроса ACTION");
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("ACTION с невалидным токеном после успешного LOGIN")
    @Description("""
            Проверяет обработку ACTION с невалидным токеном после успешной аутентификации:
            - Выполняется успешный LOGIN с валидным токеном
            - Отправляется запрос ACTION с другим (невалидным) токеном
            - Ожидается ошибка, так как токен невалидный
            """)
    @Tag(REGRESSION)
    void performActionWithInvalidTokenAfterValidLogin() {
        String validToken = TokenGenerator.generateValidToken();
        String invalidToken = TokenGenerator.generateTokenOfRandomLength();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен 1 (валидный): %s
                                        Длина: %d символов
                                        Токен 2 (невалидный): %s
                                        Длина: %d символов (требуется: 32)
                                        Сценарий:
                                        1. LOGIN с валидным токеном (ожидается успех)
                                        2. ACTION с невалидным токеном (ожидается ошибка)
                                        Ожидаемый результат:
                                        • ACTION должен быть отклонен
                                        • Статус: 400 Bad Request
                                        • Сообщение: "Неверный формат токена. Токен должен содержать 32 символа (цифры и буквы A-F)"
                                        """,
                                validToken,
                                validToken.length(),
                                invalidToken,
                                invalidToken.length())));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(validToken);
            mockDoActionSuccess(validToken);
            addTestData("Настройка тестового окружения",
                    "Сервис аутентификации и сервис выполнения действий настроены на успешный ответ");
        });

        performSuccessfulLogin(validToken);

        Allure.step("Выполнение ACTION с невалидным токеном", () -> {
            Response actionResponse = given()
                    .spec(forValidApiKey(invalidToken, ACTION_ACTION))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидаем ошибку 'неверный формат токена'", () ->
                    verifyInvalidTokenError(actionResponse));
        });
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("ACTION без предварительного LOGIN")
    @Description("""
            Проверяет обработку ACTION без предварительной аутентификации:
            - Токен никогда не проходил LOGIN
            - Отправляется запрос ACTION
            - Ожидается ошибка: токен не найден
            """)
    @Tag(REGRESSION)
    void performActionWithoutLogin() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        Длина: %d символов
                                        Сценарий: Прямой запрос ACTION без LOGIN
                                        Ожидается: ошибка 403 Forbidden
                                        """,
                                token,
                                token.length())));

        Allure.step("Выполнение запроса ACTION без предварительной аутентификации", () -> {
            Response response = performAction(token);

            Allure.step("Проверка ответа: ожидаем ошибку 'токен не найден'", () ->
                    verifyTokenNotFoundError(response, token));
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
            - Ожидается ошибка: токен не найден
            """)
    @Tag(REGRESSION)
    void performActionAfterLogout() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s
                                        Длина: %d символов
                                        Сценарий:
                                        1. LOGIN с токеном (ожидается успех)
                                        2. LOGOUT с токеном (ожидается успех)
                                        3. ACTION с тем же токеном (ожидается ошибка)
                                        """,
                                token,
                                token.length())));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            mockDoActionSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервисы настроены на успешные ответы");
        });

        performSuccessfulLogin(token);
        performSuccessfulLogout(token);

        Allure.step("Попытка выполнения ACTION после завершения сессии", () -> {
            Response actionResponse = performAction(token);

            Allure.step("Проверка ответа: ожидаем ошибку 'токен не найден'", () ->
                    verifyTokenNotFoundError(actionResponse, token));
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
            
            ВАЖНО: В текущей реализации приложение всегда возвращает 500 Internal Server Error
            независимо от кода ошибки внешнего сервиса (403, 404 или 500).
            Это поведение требует уточнения у разработчиков.
            """)
    @Tag(NEEDS_CLARIFICATION)
    @Tag(REGRESSION)
    void performActionWhenExternalServiceReturnsError(int statusCode) {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Код ошибки внешнего сервиса: %d
                                        Токен: %s (валидный)
                                        Сценарий:
                                        1. LOGIN с токеном (ожидается успех)
                                        2. ACTION при ошибке внешнего сервиса /doAction
                                        """,
                                statusCode, token)));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            mockDoActionError(token, statusCode);
            addTestData("Настройка тестового окружения",
                    String.format("Сервис выполнения действий настроен на возврат ошибки с кодом %d", statusCode));
        });

        performSuccessfulLogin(token);

        Allure.step("Выполнение ACTION при сбое внешнего сервиса", () -> {
            Response actionResponse = performAction(token);

            Allure.step(String.format("Проверка ответа: ожидаем ошибку, вызванную сбоем внешнего сервиса (код %d)",
                    statusCode), () ->
                    verifyExternalServiceError(actionResponse, statusCode));
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
            """)
    @Tag(REGRESSION)
    void performMultipleActionsWithSameToken() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s
                                        Длина: %d символов
                                        Сценарий:
                                        1. LOGIN с токеном
                                        2. ACTION №1 (ожидается успех)
                                        3. ACTION №2 (ожидается успех)
                                        4. ACTION №3 (ожидается успех)
                                        """,
                                token,
                                token.length())));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            mockDoActionSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервисы настроены на успешные ответы");
        });

        performSuccessfulLogin(token);

        Allure.step("Выполнение нескольких запросов ACTION подряд", () -> {
            for (int i = 0; i < 3; i++) {
                performSuccessfulAction(token, "ACTION №" + (i + 1));
            }
            addTestData("Итог выполнения", "Все 3 действия ACTION выполнены успешно");
        });
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("ACTION без API ключа")
    @Description("""
            Проверяет обработку запроса ACTION без обязательного заголовка X-Api-Key:
            - Токен успешно аутентифицирован
            - Отправляется запрос ACTION без заголовка авторизации
            - Ожидается ошибка: 401 Unauthorized
            """)
    @Tag(REGRESSION)
    void performActionWithoutApiKey() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        API ключ: отсутствует
                                        Сценарий:
                                        1. LOGIN с токеном (ожидается успех)
                                        2. ACTION без API ключа (ожидается ошибка)
                                        """,
                                token)));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            mockDoActionSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервисы настроены на успешные ответы");
        });

        performSuccessfulLogin(token);

        Allure.step("Выполнение ACTION без ключа доступа (API ключа)", () -> {
            Response response = given()
                    .spec(forEmptyApiKey(token, ACTION_ACTION))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидаем ошибку 'отсутствует API ключ'", () ->
                    verifyInvalidApiKeyError(response));
        });
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("ACTION с неверным API ключом")
    @Description("""
            Проверяет обработку запроса ACTION с неверным API ключом:
            - Токен успешно аутентифицирован
            - Отправляется запрос ACTION с некорректным X-Api-Key
            - Ожидается ошибка: 401 Unauthorized
            """)
    @Tag(REGRESSION)
    void performActionWithInvalidApiKey() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        API ключ: 'wrong-key' (недействительный)
                                        Сценарий:
                                        1. LOGIN с токеном (ожидается успех)
                                        2. ACTION с неверным API ключом (ожидается ошибка)
                                        """,
                                token)));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            mockDoActionSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервисы настроены на успешные ответы");
        });

        performSuccessfulLogin(token);

        Allure.step("Выполнение ACTION с некорректным ключом доступа", () -> {
            Response response = given()
                    .spec(forWrongApiKey(token, ACTION_ACTION))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидаем ошибку 'неверный API ключ'", () ->
                    verifyInvalidApiKeyError(response));
        });
    }

    @Test
    @Story("Ошибки валидации")
    @DisplayName("ACTION без параметра action")
    @Description("""
            Проверяет обработку запроса ACTION без параметра action:
            - Токен успешно аутентифицирован
            - Отправляется запрос без параметра action
            - Ожидается ошибка валидации
            """)
    @Tag(REGRESSION)
    void performActionWithoutActionParameter() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        Длина: %d символов
                                        Сценарий:
                                        1. LOGIN с токеном (ожидается успех)
                                        2. ACTION без параметра action (ожидается ошибка)
                                        """,
                                token,
                                token.length())));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервис аутентификации настроен на успешный ответ");
        });

        performSuccessfulLogin(token);

        Allure.step("Попытка выполнения ACTION без указания параметра action", () -> {
            Response actionResponse = given()
                    .spec(forValidApiKey(token, ""))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидаем ошибку 'отсутствует обязательный параметр action'", () ->
                    verifyMissingActionError(actionResponse));
        });
    }
}