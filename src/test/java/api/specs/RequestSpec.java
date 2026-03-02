package api.specs;

import api.exceptions.UtilityClassException;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static api.utils.Constants.*;

/**
 * Утилитный класс для создания спецификаций запросов к API.
 * <p>
 * Предоставляет методы для построения конфигураций HTTP-запросов с различными
 * комбинациями токенов, действий и API ключей.
 */
public class RequestSpec {

    private RequestSpec() {
        throw new UtilityClassException(getClass());
    }

    /**
     * Создает базовую спецификацию запроса с общими настройками.
     *
     * @return базовая спецификация с URI, Content-Type и Accept заголовками
     */
    public static RequestSpecification getBaseSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(BASE_URL)
                .setContentType(ContentType.URLENC)
                .setAccept(ContentType.JSON)
                .build();
    }

    /**
     * Создает спецификацию запроса для конкретного эндпоинта.
     *
     * @param token  токен аутентификации
     * @param action выполняемое действие (LOGIN, ACTION, LOGOUT)
     * @param apiKey API ключ для авторизации (может быть null)
     * @return спецификация запроса с параметрами формы и заголовком API ключа
     */
    public static RequestSpecification forEndpoint(String token, String action, String apiKey) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .addRequestSpecification(getBaseSpec())
                .addFormParam(TOKEN_PARAM, token)
                .addFormParam(ACTION_PARAM, action);

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            builder.addHeader(API_KEY_HEADER_NAME, apiKey);
        }
        return builder.build();
    }

    /**
     * Создает спецификацию с валидным API ключом.
     *
     * @param token  токен аутентификации
     * @param action выполняемое действие
     * @return спецификация с корректным API ключом
     */
    public static RequestSpecification forValidApiKey(String token, String action) {
        return forEndpoint(token, action, VALID_API_KEY);
    }

    /**
     * Создает спецификацию с невалидным API ключом для негативных тестов.
     *
     * @param token  токен аутентификации
     * @param action выполняемое действие
     * @return спецификация с некорректным API ключом
     */
    public static RequestSpecification forWrongApiKey(String token, String action) {
        return forEndpoint(token, action, INVALID_API_KEY);
    }

    /**
     * Создает спецификацию с пустым API ключом для проверки обязательности заголовка.
     *
     * @param token  токен аутентификации
     * @param action выполняемое действие
     * @return спецификация без API ключа
     */
    public static RequestSpecification forEmptyApiKey(String token, String action) {
        return forEndpoint(token, action, EMPTY_API_KEY);
    }
}