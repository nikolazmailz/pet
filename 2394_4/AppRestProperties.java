package ru.ntdev.srhr.ms.sys.delegation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <pre>
 * app:
 *   rest: { read-timeout, connect-timeout }   # миллисекунды
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.rest")
public class AppRestProperties {

    private int readTimeout;
    private int connectTimeout;
}
