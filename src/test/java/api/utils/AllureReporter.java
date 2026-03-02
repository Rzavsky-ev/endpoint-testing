package api.utils;

import api.exceptions.UtilityClassException;
import io.qameta.allure.Allure;

import static api.utils.Constants.*;

/**
 * Утилитный класс для работы с Allure отчетами.
 * <p>
 * Предоставляет методы для добавления вложений в отчеты и
 * преобразования HTTP статус кодов в понятные текстовые описания.
 */
public class AllureReporter {

    private AllureReporter() {
        throw new UtilityClassException(getClass());
    }

    /**
     * Добавляет текстовое вложение в Allure отчет.
     *
     * @param title   заголовок вложения
     * @param content текст вложения
     */
    public static void addTestData(String title, String content) {
        Allure.addAttachment(title, TEXT_PLAIN, content);
    }

    /**
     * Возвращает текстовое описание HTTP статус кода.
     *
     * @param statusCode HTTP статус код
     * @return понятное описание статуса
     */
    public static String getStatusText(int statusCode) {
        return switch (statusCode) {
            case 200 -> RESULT_OK;
            case 400 -> BAD_REQUEST_MESSAGE;
            case 401 -> UNAUTHORIZED_MESSAGE;
            case 403 -> FORBIDDEN_MESSAGE;
            case 404 -> NOT_FOUND_MESSAGE;
            case 409 -> CONFLICT_MESSAGE;
            case 500 -> INTERNAL_MESSAGE;
            default -> UNKNOWN_MESSAGE;
        };
    }
}