
package api.tests;

import api.base.BaseTest;
import api.utils.TokenGenerator;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static api.specs.RequestSpec.*;
import static api.utils.ActionSteps.*;
import static api.utils.AllureReporter.addTestData;
import static api.utils.Constants.*;
import static api.utils.ResponseVerifier.*;
import static api.utils.WireMockStubBuilder.*;
import static io.restassured.RestAssured.given;

@Epic("Тестирование веб-сервиса")
@Feature("Функционал LOGOUT")
@DisplayName("Тесты для выхода из системы - LOGOUT")
public class LogoutTest extends BaseTest {

    @Test
    @Story("Успешные сценарии")
    @DisplayName("Успешный выход из системы после входа")
    @Description("""
            Проверяет успешное завершение сессии после входа в систему:
            - Вход в систему активирует токен
            - Выход из системы удаляет токен
            - После выхода токен перестает работать
            - Система возвращает сообщение об успехе: {"result":"OK"} для LOGOUT
            """)
    @Tag(SMOKE)
    void successfulLogoutAfterLogin() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s
                                        Длина: %d символов
                                        Сценарий:
                                        1. Вход в систему с токеном (ожидается успех)
                                        2. Выход из системы с тем же токеном (ожидается успех)
                                        3. Проверка, что токен больше не работает (ожидается ошибка)
                                        """,
                                token,
                                token.length())));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервис проверки токенов настроен на успешный ответ");
        });

        performSuccessfulLogin(token);
        performSuccessfulLogout(token);

        Allure.step("Проверка, что токен больше не работает после выхода", () -> {
            Response actionResponse = performAction(token);

            Allure.step(String.format("Проверка ответа: ожидается ошибка 'Token %s not found'", token), () ->
                    verifyTokenNotFoundError(actionResponse, token));
            addTestData("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Токен больше не работает после выхода из системы");
        });
    }

    @Test
    @Story("Ошибки валидации")
    @DisplayName("Выход из системы с некорректным токеном после успешного входа")
    @Description("""
            Проверяет обработку запроса на выход с некорректным токеном:
            - Выполняется успешный вход с валидным токеном
            - Отправляется запрос на выход с другим (невалидным) токеном
            - Ожидается ответ: {"result": "ERROR",
            "message":"token: должно соответствовать \\"^[0-9A-F]{32}$\\""}
            """)
    @Tag(REGRESSION)
    void logoutWithInvalidTokenAfterValidLogin() {
        String validToken = TokenGenerator.generateValidToken();
        String invalidToken = TokenGenerator.generateTokenOfRandomLength();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен 1 (валидный, для входа): %s
                                        Длина: %d символов
                                        Токен 2 (некорректный, для выхода): %s
                                        Длина: %d символов (требуется: 32)
                                        Сценарий:
                                        1. Вход с валидным токеном (ожидается успех)
                                        2. Попытка выхода с некорректным токеном (ожидается ошибка)
                                        Ожидается: ошибка с описанием причины
                                        """,
                                validToken,
                                validToken.length(),
                                invalidToken,
                                invalidToken.length())));

        Allure.step("Имитация работы внешнего сервиса", () -> {
            mockAuthSuccess(validToken);
            addTestData("Настройка тестового окружения",
                    "Сервис проверки токенов настроен на успешный ответ");
        });

        performSuccessfulLogin(validToken);

        Allure.step("Попытка выхода из системы с некорректным токеном", () -> {
            Response logoutResponse = performLogout(invalidToken);

            Allure.step("Проверка ответа: ожидается ошибка 'token: должно соответствовать \"^[0-9A-F]{32}$\"'", () ->
                    verifyInvalidTokenError(logoutResponse));
            addTestData("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Система отклонила запрос на выход с некорректным токеном");
        });
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("Выход из системы без предварительного входа")
    @Description("""
            Проверяет обработку запроса на выход без предварительной аутентификации:
            - Токен никогда не использовался для входа в систему
            - Отправляется запрос на выход
            - Ожидается ответ: {"result": "ERROR", "message":"Token '<token>' not found"}
            """)
    @Tag(REGRESSION)
    void performLogoutWithoutLogin() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s
                                        Длина: %d символов
                                        Сценарий: Прямой запрос на выход без предварительного входа
                                        Ожидается: ошибка с описанием причины
                                        """,
                                token,
                                token.length())));

        Allure.step("Выполнение запроса на выход без предварительного входа", () -> {
            Response response = performLogout(token);

            Allure.step(String.format("Проверка ответа: ожидается ошибка 'Token %s not found'", token), () ->
                    verifyTokenNotFoundError(response, token));
            addTestData("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Система отклонила запрос на выход с токеном, не прошедшим LOGIN");
        });
    }

    @Test
    @Story("Многократные операции")
    @DisplayName("Несколько попыток выхода подряд с одним токеном")
    @Description("""
            Проверяет поведение системы при нескольких запросах на выход подряд:
            - Выполняется успешный вход с токеном
            - Первый запрос на выход выполняется успешно
            - Второй запрос на выход отклоняется (токен уже удален)
            - Ожидается ответ: {"result":"OK"} для первого, {"result":"ERROR"} для второго
            """)
    @Tag(REGRESSION)
    void performMultipleLogoutsWithSameToken() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s
                                        Длина: %d символов
                                        Сценарий:
                                        1. Вход в систему с токеном (ожидается успех)
                                        2. Первый выход из системы (ожидается успех)
                                        3. Второй выход из системы (ожидается ошибка)
                                        """,
                                token,
                                token.length())));

        Allure.step("Имитация работы внешнего сервиса", () -> {
            mockAuthSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервис проверки токенов настроен на успешный ответ");
        });

        performSuccessfulLogin(token);
        performSuccessfulLogout(token);

        Allure.step("Повторная попытка выхода из системы", () -> {
            Response secondLogout = performLogout(token);

            Allure.step(String.format("Проверка ответа: ожидается ошибка 'Token %s not found'", token), () ->
                    verifyTokenNotFoundError(secondLogout, token));
            addTestData("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Повторный выход отклонен (токен уже удален)");
        });
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("Выход из системы без ключа доступа")
    @Description("""
            Проверяет обработку запроса на выход без обязательного заголовка X-Api-Key:
            - Выполняется успешный вход в систему с валидным токеном
            - Отправляется запрос на выход без заголовка авторизации
            - Ожидается ответ: {"result": "ERROR", "message":"Missing or invalid API Key"}
            """)
    @Tag(REGRESSION)
    void logoutWithoutApiKey() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        Ключ доступа: отсутствует
                                        Сценарий:
                                        1. Вход в систему с токеном (ожидается успех)
                                        2. Попытка выхода без ключа доступа (ожидается ошибка)
                                        Ожидается: ошибка с описанием причины
                                        """,
                                token)));

        Allure.step("Имитация работы внешнего сервиса", () -> {
            mockAuthSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервис проверки токенов настроен на успешный ответ");
        });

        performSuccessfulLogin(token);

        Allure.step("Попытка выхода из системы без ключа доступа", () -> {
            Response response = given()
                    .spec(forEmptyApiKey(token, ACTION_LOGOUT))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'Missing or invalid API Key'", () ->
                    verifyInvalidApiKeyError(response));
            addTestData("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Неверный API ключ отклонен");
        });
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("Выход из системы с неверным ключом доступа")
    @Description("""
            Проверяет обработку запроса на выход с некорректным ключом доступа:
            - Выполняется успешный вход в систему с валидным токеном
            - Отправляется запрос на выход с недействительным X-Api-Key
            - Ожидается ответ: {"result": "ERROR", "message":"Missing or invalid API Key"}
            """)
    @Tag(REGRESSION)
    void logoutWithInvalidApiKey() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s (валидный)
                                        Ключ доступа: 'wrong-key' (недействительный)
                                        Сценарий:
                                        1. Вход в систему с токеном (ожидается успех)
                                        2. Попытка выхода с неверным ключом доступа (ожидается ошибка)
                                        Ожидается: ошибка с описанием причины
                                        """,
                                token)));

        Allure.step("Имитация работы внешнего сервиса", () -> {
            mockAuthSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервис проверки токенов настроен на успешный ответ");
        });

        performSuccessfulLogin(token);

        Allure.step("Попытка выхода из системы с некорректным ключом доступа", () -> {
            Response response = given()
                    .spec(forWrongApiKey(token, ACTION_LOGOUT))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'Missing or invalid API Key'", () ->
                    verifyInvalidApiKeyError(response));
            addTestData("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Неверный API ключ отклонен");
        });
    }

    @Test
    @Story("Комплексные сценарии")
    @DisplayName("Полный жизненный цикл токена: Вход -> Действие -> Выход")
    @Description("""
            Проверяет полный жизненный цикл токена в системе:
            - Успешный вход в систему (LOGIN)
            - Выполнение действия с токеном (ACTION)
            - Завершение сессии (LOGOUT)
            - Проверка, что токен больше не работает после выхода
            - Ожидается: {"result":"OK"} для первых трех операций, {"result":"ERROR"} для последней
            """)
    @Tag(REGRESSION)
    void fullTokenLifecycle() {
        String token = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен: %s
                                        Длина: %d символов
                                        Сценарий (полный цикл):
                                        1. Вход в систему с токеном (ожидается успех)
                                        2. Выполнение действия с токеном (ожидается успех)
                                        3. Выход из системы с токеном (ожидается успех)
                                        4. Попытка действия после выхода (ожидается ошибка)
                                        """,
                                token,
                                token.length())));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token);
            mockDoActionSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервисы проверки токенов и выполнения действий настроены на успешные ответы");
        });

        performSuccessfulLogin(token);
        performSuccessfulAction(token);
        performSuccessfulLogout(token);

        Allure.step("Попытка выполнения действия после выхода из системы", () -> {
            Response finalActionResponse = performAction(token);

            Allure.step(String.format("Проверка ответа: ожидается ошибка 'Token %s not found'", token), () ->
                    verifyTokenNotFoundError(finalActionResponse, token));
            addTestData("РЕЗУЛЬТАТ ПРОВЕРКИ",
                    "✓ Действие после выхода отклонено");
        });

        Allure.step("Итог тестирования", () ->
                addTestData("РЕЗУЛЬТАТ ТЕСТА",
                        """
                                ПРОВЕРКА ПОЛНОГО ЖИЗНЕННОГО ЦИКЛА ТОКЕНА
                                
                                Что проверено:
                                ✓ Подготовка валидного токена
                                ✓ Успешный вход в систему (LOGIN)
                                ✓ Успешное выполнение действия (ACTION)
                                ✓ Успешный выход из системы (LOGOUT)
                                ✓ Отказ в выполнении действия после выхода
                                
                                ВЫВОД: Все этапы жизненного цикла токена работают корректно.
                                """));
    }

    @Test
    @Story("Многопользовательские сценарии")
    @DisplayName("Выход из системы для разных токенов")
    @Description("""
            Проверяет изоляцию сессий при работе с несколькими токенами:
            - Вход в систему выполнен для двух разных токенов
            - Выполняется выход для первого токена
            - Проверяется, что второй токен продолжает работать
            - Сессии должны быть изолированы друг от друга
            """)
    @Tag(REGRESSION)
    void logoutDifferentTokens() {
        String token1 = TokenGenerator.generateValidToken();
        String token2 = TokenGenerator.generateValidToken();

        Allure.step("Подготовка тестовых данных", () ->
                addTestData("Тестовый сценарий",
                        String.format("""
                                        Токен 1 (пользователь 1): %s
                                        Токен 2 (пользователь 2): %s
                                        Длина обоих токенов: 32 символа
                                        Сценарий:
                                        1. Вход в систему для обоих токенов (ожидается успех)
                                        2. Выход из системы для токена 1 (ожидается успех)
                                        3. Попытка действия с токеном 1 (ожидается ошибка)
                                        4. Действие с токеном 2 (ожидается успех)
                                        
                                        Ожидается: сессии пользователей изолированы
                                        """,
                                token1, token2)));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token1);
            mockAuthSuccess(token2);
            mockDoActionSuccess(token2);
            addTestData("Настройка тестового окружения",
                    "Сервисы настроены на успешные ответы для двух токенов");
        });

        Allure.step("Вход в систему для обоих пользователей", () -> {
            Allure.step("Вход пользователя 1", () -> {
                Response response1 = performLogin(token1);
                verifySuccess(response1);
                addTestData("Пользователь 1", "✓ Вход выполнен");
            });

            Allure.step("Вход пользователя 2", () -> {
                Response response2 = performLogin(token2);
                verifySuccess(response2);
                addTestData("Пользователь 2", "✓ Вход выполнен");
            });
        });

        performSuccessfulLogout(token1);

        Allure.step("Проверка состояния токенов после выхода пользователя 1", () -> {
            Allure.step("Пользователь 1 пытается выполнить действие", () -> {
                Response action1Response = performAction(token1);
                verifyTokenNotFoundError(action1Response, token1);
                addTestData("Пользователь 1", "✓ Действие отклонено - сессия завершена");
            });

            Allure.step("Пользователь 2 выполняет действие", () -> {
                Response action2Response = performAction(token2);
                verifySuccess(action2Response);
                addTestData("Пользователь 2", "✓ Действие выполнено - сессия активна");
            });
        });

        Allure.step("Итог тестирования", () ->
                addTestData("РЕЗУЛЬТАТ ТЕСТА",
                        """
                                ПРОВЕРКА ИЗОЛЯЦИИ СЕССИЙ
                                
                                Проверено и подтверждено:
                                ✓ Аутентификация двух разных пользователей
                                ✓ Завершение сессии первого пользователя
                                ✓ Первый пользователь больше не может выполнять действия
                                ✓ Второй пользователь продолжает работать
                                ✓ Сессии пользователей полностью изолированы
                                
                                ВЫВОД: система корректно обрабатывает множественные сессии
                                и обеспечивает изоляцию между разными пользователями.
                                """));
    }
}