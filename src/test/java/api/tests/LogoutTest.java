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
@DisplayName("Тесты для выхода из системы (LOGOUT)")
public class LogoutTest extends BaseTest {

    @Test
    @Story("Успешные сценарии")
    @DisplayName("Успешный выход из системы после входа")
    @Description("""
            Проверяет успешное завершение сессии после входа в систему:
            - Вход в систему активирует токен
            - Выход из системы удаляет токен
            - После выхода токен перестает работать
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
            mockDoActionSuccess(token);
            addTestData("Настройка тестового окружения",
                    "Сервисы проверки токенов и выполнения действий настроены на успешные ответы");
        });

        performSuccessfulLogin(token);
        performSuccessfulLogout(token);

        Allure.step("Проверка, что токен больше не работает после выхода", () -> {
            Response actionResponse = given()
                    .spec(forValidApiKey(token, ACTION_ACTION))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'токен не найден'", () ->
                    verifyTokenNotFoundError(actionResponse, token));
        });
    }

    @Test
    @Story("Ошибки валидации")
    @DisplayName("Выход из системы с некорректным токеном после успешного входа")
    @Description("""
            Проверяет ситуацию:
            1. Вход выполнен с валидным токеном (токен зарегистрирован в системе)
            2. Запрос на выход отправляется с другим некорректным токеном
            Ожидается: ошибка, так как токен для выхода не соответствует формату
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
                                        Ожидаемый результат:
                                        • Выход должен быть отклонен
                                        • Статус: 400 Bad Request
                                        • Сообщение: неверный формат токена
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
            Response logoutResponse = given()
                    .spec(forValidApiKey(invalidToken, ACTION_LOGOUT))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'неверный формат токена'", () ->
                    verifyInvalidTokenError(logoutResponse));
        });
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("Выход из системы без предварительного входа")
    @Description("""
            Проверяет обработку запроса на выход без предварительной аутентификации:
            - Токен никогда не использовался для входа в систему
            - Отправляется запрос на выход
            - Ожидается ошибка: токен не найден в системе
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
                                        Ожидаемый результат: ошибка 403 Forbidden
                                        """,
                                token,
                                token.length())));

        Allure.step("Выполнение запроса на выход без предварительного входа", () -> {
            Response response = given()
                    .spec(forValidApiKey(token, ACTION_LOGOUT))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'токен не найден'", () ->
                    verifyTokenNotFoundError(response, token));
        });
    }

    @Test
    @Story("Многократные операции")
    @DisplayName("Несколько попыток выхода подряд с одним токеном")
    @Description("""
            Проверяет возможность выполнения нескольких запросов на выход подряд:
            - Токен успешно аутентифицирован
            - Выполняется первый выход из системы (успешно)
            - Выполняется второй выход из системы (должен завершиться ошибкой, так как токен уже удален)
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
            Response secondLogout = given()
                    .spec(forValidApiKey(token, ACTION_LOGOUT))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'токен не найден'", () ->
                    verifyTokenNotFoundError(secondLogout, token));
        });
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("Выход из системы без ключа доступа")
    @Description("""
            Проверяет обработку запроса на выход без обязательного ключа доступа (API ключа):
            - Выполняется успешный вход в систему с валидным токеном
            - Отправляется запрос на выход без заголовка авторизации
            - Ожидается ошибка: 401 Unauthorized
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

            Allure.step("Проверка ответа: ожидается ошибка 'отсутствует ключ доступа'", () ->
                    verifyInvalidApiKeyError(response));
        });
    }

    @Test
    @Story("Ошибки авторизации")
    @DisplayName("Выход из системы с неверным ключом доступа")
    @Description("""
            Проверяет обработку запроса на выход с некорректным ключом доступа:
            - Выполняется успешный вход в систему с валидным токеном
            - Отправляется запрос на выход с недействительным X-Api-Key
            - Ожидается ошибка: 401 Unauthorized
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

            Allure.step("Проверка ответа: ожидается ошибка 'неверный ключ доступа'", () ->
                    verifyInvalidApiKeyError(response));
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
        performSuccessfulAction(token, "Выполнение действия с токеном");
        performSuccessfulLogout(token);

        Allure.step("Попытка выполнения действия после выхода из системы", () -> {
            Response finalActionResponse = given()
                    .spec(forValidApiKey(token, ACTION_ACTION))
                    .when()
                    .post(ENDPOINT);

            Allure.step("Проверка ответа: ожидается ошибка 'токен не найден'", () ->
                    verifyTokenNotFoundError(finalActionResponse, token));
        });

        Allure.step("Итог тестирования", () -> {
            String resultText = """
                    ТЕСТ УСПЕШНО ЗАВЕРШЕН
                    
                    Что проверено (полный цикл работы токена):
                    - ✓ Подготовка валидного токена
                    - ✓ Успешный вход в систему (LOGIN)
                    - ✓ Успешное выполнение действия (ACTION)
                    - ✓ Успешный выход из системы (LOGOUT)
                    - ✓ Отказ в выполнении действия после выхода
                    - ✓ Корректный код ошибки 403
                    - ✓ Понятное сообщение об ошибке
                    
                    Все этапы жизненного цикла токена работают корректно.
                    """;
            addTestData("Результат теста", resultText);
        });
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
                                        
                                        Ожидаемый результат: сессии пользователей изолированы
                                        """,
                                token1, token2)));

        Allure.step("Имитация работы внешних сервисов", () -> {
            mockAuthSuccess(token1);
            mockAuthSuccess(token2);
            mockDoActionSuccess(token1);
            mockDoActionSuccess(token2);
            addTestData("Настройка тестового окружения",
                    "Сервисы настроены на успешные ответы для двух токенов");
        });

        Allure.step("Вход в систему для обоих пользователей", () -> {
            Allure.step("Вход пользователя 1 (токен 1)", () -> {
                Response response1 = performLogin(token1);
                verifyLoginSuccess(response1);
            });

            Allure.step("Вход пользователя 2 (токен 2)", () -> {
                Response response2 = performLogin(token2);
                verifyLoginSuccess(response2);
            });
        });

        performSuccessfulLogout(token1);

        Allure.step("Проверка состояния токенов после выхода пользователя 1", () -> {
            Allure.step("Пользователь 1 пытается выполнить действие (ожидается ошибка)", () -> {
                Response action1Response = performAction(token1);
                verifyTokenNotFoundError(action1Response, token1);
                addTestData("Проверка токена 1", "✓ Токен 1 больше не работает - сессия завершена");
            });

            Allure.step("Пользователь 2 выполняет действие (ожидается успех)", () -> {
                Response action2Response = performAction(token2);
                verifyActionSuccess(action2Response, "Действие пользователя 2");
                addTestData("Проверка токена 2", "✓ Токен 2 продолжает работать - сессия активна");
            });
        });

        Allure.step("Итог тестирования многопользовательского сценария", () -> {
            String resultText = """
                    МНОГОПОЛЬЗОВАТЕЛЬСКИЙ СЦЕНАРИЙ УСПЕШНО ВЫПОЛНЕН
                    
                    Проверено и подтверждено:
                    ✓ Аутентификация двух разных пользователей
                    ✓ Завершение сессии первого пользователя
                    ✓ Первый пользователь больше не может выполнять действия
                    ✓ Второй пользователь продолжает работать
                    ✓ Сессии пользователей полностью изолированы
                    
                    Вывод: система корректно обрабатывает множественные сессии
                    и обеспечивает изоляцию между разными пользователями.
                    """;
            addTestData("Результат теста", resultText);
        });
    }
}