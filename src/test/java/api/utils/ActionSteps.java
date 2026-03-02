package api.utils;

import api.exceptions.UtilityClassException;
import io.qameta.allure.Allure;
import io.restassured.response.Response;

import static api.specs.RequestSpec.forValidApiKey;
import static api.utils.Constants.*;
import static io.restassured.RestAssured.given;

/**
 * Утилитный класс для выполнения основных действий в тестах.
 * <p>
 * Содержит методы для выполнения запросов (LOGIN, ACTION, LOGOUT) и
 * готовые шаги с проверками для успешных сценариев.
 */
public class ActionSteps {

    private ActionSteps() {
        throw new UtilityClassException(getClass());
    }

    /**
     * Выполняет запрос на вход в систему (LOGIN).
     *
     * @param token токен аутентификации
     * @return Response объект с ответом от сервера
     */
    public static Response performLogin(String token) {
        return given()
                .spec(forValidApiKey(token, ACTION_LOGIN))
                .when()
                .post(ENDPOINT);
    }

    /**
     * Выполняет запрос действия (ACTION).
     *
     * @param token токен аутентификации
     * @return Response объект с ответом от сервера
     */
    public static Response performAction(String token) {
        return given()
                .spec(forValidApiKey(token, ACTION_ACTION))
                .when()
                .post(ENDPOINT);
    }

    /**
     * Выполняет запрос на выход из системы (LOGOUT).
     *
     * @param token токен аутентификации
     * @return Response объект с ответом от сервера
     */
    public static Response performLogout(String token) {
        return given()
                .spec(forValidApiKey(token, ACTION_LOGOUT))
                .when()
                .post(ENDPOINT);
    }

    /**
     * Выполняет успешный вход в систему с проверкой.
     * Добавляет шаг в Allure отчет.
     *
     * @param token валидный токен
     */
    public static void performSuccessfulLogin(String token) {
        Allure.step("Вход в систему с валидным токеном", () -> {
            Response response = performLogin(token);
            ResponseVerifier.verifyLoginSuccess(response);
        });
    }

    /**
     * Выполняет успешное действие с проверкой.
     * Добавляет шаг в Allure отчет.
     *
     * @param token    валидный токен
     * @param stepName название шага для отчета
     */
    public static void performSuccessfulAction(String token, String stepName) {
        Allure.step(stepName, () -> {
            Response response = performAction(token);
            ResponseVerifier.verifyActionSuccess(response, stepName);
        });
    }

    /**
     * Выполняет успешный выход из системы с проверкой.
     * Добавляет шаг в Allure отчет.
     *
     * @param token валидный токен
     */
    public static void performSuccessfulLogout(String token) {
        Allure.step("Выход из системы", () -> {
            Response response = performLogout(token);
            ResponseVerifier.verifyLogoutSuccess(response);
        });
    }
}