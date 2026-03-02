package api.utils;

import api.exceptions.UtilityClassException;
import com.github.javafaker.Faker;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import static api.utils.Constants.TOKEN_LENGTH;

/**
 * Утилитный класс для генерации тестовых токенов.
 * Содержит методы создания валидных и невалидных токенов,
 * а также DataProvider для параметризованных тестов.
 */
public class TokenGenerator {

    /**
     * Шаблон описания для токенов с указанием требуемой длины.
     */
    private static final String TEMPLATE_WITH_REQUIRED_LENGTH = """
            Токен: %s
            Длина: %d символов (требуется: 32)
            Ожидается: ошибка валидации 400
            """;

    /**
     * Шаблон простого описания для невалидных токенов.
     */
    private static final String TEMPLATE_SIMPLE = """
            Токен: %s
            Длина: %d символов
            Ожидается: ошибка валидации 400
            """;

    private static final Faker faker = new Faker();

    private TokenGenerator() {
        throw new UtilityClassException(getClass());
    }

    /**
     * Генерирует валидный токен (32 символа, A-F0-9).
     */
    public static String generateValidToken() {
        return faker.regexify("[A-F0-9]{" + TOKEN_LENGTH + "}");
    }

    /**
     * Генерирует токен случайной длины (1-31 символ).
     */
    public static String generateTokenOfRandomLength() {
        int length = faker.random().nextInt(1, TOKEN_LENGTH - 1);
        return faker.regexify("[A-F0-9]{" + length + "}");
    }

    /**
     * Генерирует токен со строчной буквой в конце.
     */
    public static String generateLowerCaseToken() {
        return faker.regexify("[A-F0-9]{" + (TOKEN_LENGTH - 1) + "}") + "a";
    }

    /**
     * Генерирует токен из спецсимволов.
     */
    public static String generateTokenWithSpecialChars() {
        return faker.regexify("[@#$%^&*]{" + TOKEN_LENGTH + "}");
    }

    /**
     * Форматирует описание для токена с указанием требуемой длины.
     */
    private static String formatDescriptionWithRequiredLength(String token) {
        return String.format(TEMPLATE_WITH_REQUIRED_LENGTH, token, token.length());
    }

    /**
     * Форматирует простое описание для токена.
     */
    private static String formatSimpleDescription(String token) {
        return String.format(TEMPLATE_SIMPLE, token, token.length());
    }

    /**
     * Возвращает описание для пустого токена.
     */
    private static String getEmptyTokenDescription() {
        return """
                Токен: '' (пустая строка)
                Длина: 0 символов (требуется: 32)
                Ожидается: ошибка валидации 400
                """;
    }

    /**
     * Возвращает описание для null токена.
     */
    private static String getNullTokenDescription() {
        return """
                Токен: null (отсутствующее значение)
                Ожидается: ошибка валидации 400
                """;
    }

    /**
     * DataProvider для параметризованных тестов с невалидными токенами.
     */
    static Stream<Arguments> invalidTokenDataProvider() {
        String shortToken = generateTokenOfRandomLength();
        String lowerCaseToken = generateLowerCaseToken();
        String specialCharsToken = generateTokenWithSpecialChars();

        return Stream.of(
                Arguments.of("короткий токен", shortToken,
                        formatDescriptionWithRequiredLength(shortToken)),
                Arguments.of("токен со строчными буквами", lowerCaseToken,
                        formatSimpleDescription(lowerCaseToken)),
                Arguments.of("токен со спецсимволами", specialCharsToken,
                        formatSimpleDescription(specialCharsToken)),
                Arguments.of("пустой токен", "", getEmptyTokenDescription()),
                Arguments.of("null токен", null, getNullTokenDescription())
        );
    }
}