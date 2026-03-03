package api.specs;

import api.exceptions.UtilityClassException;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;

import static api.utils.Constants.*;
import static org.hamcrest.Matchers.*;

/**
 * Утилитный класс для создания спецификаций ответов API.
 * <p>
 * Содержит готовые спецификации для проверки успешных и ошибочных ответов,
 * а также специализированные спецификации для конкретных ошибок.
 */
public class ResponseSpec {

    private ResponseSpec() {
        throw new UtilityClassException(getClass());
    }

    /**
     * Спецификация для успешного ответа (HTTP 200).
     *
     * @return спецификация с ожиданием: статус 200, ContentType JSON
     */
    public static ResponseSpecification forSuccess() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HTTP_OK)
                .expectContentType(ContentType.JSON)
                .expectBody(RESULT_PARAM, equalTo(RESULT_OK))
                .expectBody(MESSAGE_PARAM, nullValue())
                .build();
    }

    /**
     * Базовая спецификация для ошибочного ответа с заданным кодом статуса.
     *
     * @param expectedStatusCode ожидаемый HTTP статус код
     * @return спецификация с ожиданием: указанный статус, ContentType JSON
     */
    public static ResponseSpecification forError(int expectedStatusCode) {
        return new ResponseSpecBuilder()
                .expectStatusCode(expectedStatusCode)
                .expectContentType(ContentType.JSON)
                .expectBody(RESULT_PARAM, equalTo(RESULT_ERROR))
                .expectBody(MESSAGE_PARAM, not(emptyOrNullString()))
                .build();
    }

    /**
     * Спецификация для ошибки валидации (HTTP 400).
     *
     * @return спецификация с кодом 400 и базовыми проверками ошибки
     */
    public static ResponseSpecification forValidationError() {
        return forError(HTTP_BAD_REQUEST);
    }

    /**
     * Спецификация для ошибки "токен не найден" (HTTP 403).
     *
     * @param token токен, который не был найден в системе
     * @return спецификация с кодом 403 и конкретным сообщением об ошибке
     */
    public static ResponseSpecification forTokenNotFoundError(String token) {
        return new ResponseSpecBuilder()
                .addResponseSpecification(forError(HTTP_FORBIDDEN))
                .expectBody(MESSAGE_PARAM, equalTo(String.format(TOKEN_NOT_FOUND_ERROR, token)))
                .build();
    }

    /**
     * Спецификация для ошибки невалидного API ключа (HTTP 401).
     *
     * @return спецификация с кодом 401 и сообщением о неверном ключе
     */
    public static ResponseSpecification forInvalidApiKeyError() {
        return new ResponseSpecBuilder()
                .addResponseSpecification(forError(HTTP_UNAUTHORIZED))
                .expectBody(MESSAGE_PARAM, equalTo(INVALID_API_KEY_ERROR))
                .build();
    }
}