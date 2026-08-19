package ru.ntdev.srhr.common.web.sysparams;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Регистрирует {@link SapSystemParamsResolver} в Spring MVC.
 *
 * <p>Активация — через {@code @Import(SapSystemParamsConfiguration.class)}.
 * Рекомендуется добавить в список {@code @Import} аннотации
 * {@code @EnableSrhrJwtSecurity}: резолвер читает атрибуты,
 * которые устанавливает {@code JwtTokenFilter}, и без фильтра не имеет смысла.
 */
@Configuration(proxyBeanMethods = false)
public class SapSystemParamsConfiguration {

    @Bean
    public SapSystemParamsResolver sapSystemParamsResolver() {
        return new SapSystemParamsResolver();
    }

    @Bean
    public WebMvcConfigurer sapSystemParamsWebMvcConfigurer(SapSystemParamsResolver resolver) {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(resolver);
            }
        };
    }
}
