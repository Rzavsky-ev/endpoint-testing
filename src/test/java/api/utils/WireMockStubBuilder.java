package api.utils;

import api.exceptions.UtilityClassException;

import static api.utils.Constants.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Утилитный класс для настройки заглушек WireMock.
 * Содержит методы для имитации успешных и ошибочных ответов
 * внешних сервисов аутентификации и выполнения действий.
 */
public class WireMockStubBuilder {

    private WireMockStubBuilder() {
        throw new UtilityClassException(getClass());
    }

    /**
     * Настраивает успешный ответ для эндпоинта /auth.
     */
    public static void mockAuthSuccess(String token) {
        buildStub(MOCK_AUTH, token, HTTP_OK, AUTH_SUCCESS_BODY);
    }

    /**
     * Настраивает ошибочный ответ для эндпоинта /auth с заданным кодом.
     */
    public static void mockAuthError(String token, int statusCode) {
        buildStub(MOCK_AUTH, token, statusCode, AUTH_ERROR_BODY);
    }

    /**
     * Настраивает успешный ответ для эндпоинта /doAction.
     */
    public static void mockDoActionSuccess(String token) {
        buildStub(MOCK_DO_ACTION, token, HTTP_OK, ACTION_SUCCESS_BODY);
    }

    /**
     * Настраивает ошибочный ответ для эндпоинта /doAction с заданным кодом.
     */
    public static void mockDoActionError(String token, int statusCode) {
        buildStub(MOCK_DO_ACTION, token, statusCode, ACTION_ERROR_BODY);
    }

    /**
     * Базовый метод для создания заглушки WireMock.
     *
     * @param endpoint   эндпоинт (/auth или /doAction)
     * @param token      токен для проверки
     * @param statusCode HTTP статус ответа
     * @param body       тело ответа
     */
    private static void buildStub(String endpoint, String token, int statusCode, String body) {
        stubFor(post(urlEqualTo(endpoint))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_URLENCODED))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
                .withRequestBody(containing(TOKEN_PARAM + "=" + token))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }
}