package ru.ntdev.srhr.common.web.sysparams;

/**
 * Выбрасывается, когда в атрибутах запроса отсутствует (или пуст)
 * обязательный системный параметр.
 *
 * <p>Отсутствие атрибута на аутентифицированном запросе означает,
 * что {@code JwtTokenFilter} не отработал или отработал частично
 * (ранний return) — это ошибка конфигурации сервиса, а не клиента.
 * Маппинг на HTTP-статус выполняется в {@code @RestControllerAdvice}
 * конкретного сервиса.
 */
public class MissingSystemParamException extends RuntimeException {

    private final String paramName;

    public MissingSystemParamException(String paramName) {
        super("Обязательный системный параметр отсутствует в атрибутах запроса: " + paramName);
        this.paramName = paramName;
    }

    public String getParamName() {
        return paramName;
    }
}
