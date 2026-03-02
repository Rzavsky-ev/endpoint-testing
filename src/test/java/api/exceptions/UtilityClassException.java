package api.exceptions;

/**
 * Исключение, выбрасываемое при попытке создания экземпляра утилитного класса.
 */
public class UtilityClassException extends RuntimeException {

    /**
     * Создает исключение с сообщением, содержащим имя утилитного класса.
     *
     * @param utilityClass утилитный класс, для которого запрещено создание экземпляров.
     */
    public UtilityClassException(Class<?> utilityClass) {
        super(String.format(
                "Класс '%s' является утилитным. Создание экземпляров запрещено.",
                utilityClass.getSimpleName()
        ));
    }
}