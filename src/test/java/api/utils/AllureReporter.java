package api.utils;

import api.exceptions.UtilityClassException;
import io.qameta.allure.Allure;

import static api.utils.Constants.*;

/**
 * Утилитный класс для работы с Allure отчетами.
 * <p>
 * Предоставляет методы для добавления вложений в отчеты Allure.
 * Все отчеты структурированы для удобства восприятия как техническими,
 * так и нетехническими специалистами.
 */
public class AllureReporter {

    private AllureReporter() {
        throw new UtilityClassException(getClass());
    }

    /**
     * Добавляет текстовое вложение в Allure отчет.
     *
     * @param title   заголовок вложения (отображается в отчете)
     * @param content текст вложения (содержимое файла)
     */
    public static void addTestData(String title, String content) {
        Allure.addAttachment(title, TEXT_PLAIN, content);
    }
}