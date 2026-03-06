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
 * Содержит готовые спецификации для проверки успешных и ошибочных ответов.
 * Все спецификации включают проверку Content-Type и структуры JSON ответа.
 */
public class ResponseSpec {

    private ResponseSpec() {
        throw new UtilityClassException(getClass());
    }

    /**
     * Создает спецификацию для успешного ответа.
     *
     * @return спецификация для проверки успешного ответа
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
     * Создает спецификацию для ошибочного ответа.
     *
     * @return спецификация для проверки ошибочного ответа
     */
    public static ResponseSpecification forError() {
        return new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .expectBody(RESULT_PARAM, equalTo(RESULT_ERROR))
                .build();
    }
}