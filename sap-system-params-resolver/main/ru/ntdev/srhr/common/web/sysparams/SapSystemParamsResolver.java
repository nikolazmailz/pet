package ru.ntdev.srhr.common.web.sysparams;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import ru.ntdev.srhr.common.dto.SapSystemParamsDto;

/**
 * Заполняет {@link SapSystemParamsDto} из атрибутов текущего запроса
 * для аргументов, помеченных {@link SapSystemParams}.
 *
 * <p>Все четыре параметра обязательны; при отсутствии или пустом значении
 * выбрасывается {@link MissingSystemParamException} с именем параметра.
 */
public class SapSystemParamsResolver implements HandlerMethodArgumentResolver {

    static final String ATTR_CHANNEL = "channel";
    static final String ATTR_AD_LOGIN = "adLogin";
    static final String ATTR_SESSION_ID = "sessionId";
    static final String ATTR_REALM = "realm";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(SapSystemParams.class)
                && SapSystemParamsDto.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        SapSystemParamsDto systemParams = new SapSystemParamsDto();
        systemParams.setChannel(requiredAttribute(webRequest, ATTR_CHANNEL));
        systemParams.setAdLogin(requiredAttribute(webRequest, ATTR_AD_LOGIN));
        systemParams.setSessionId(requiredAttribute(webRequest, ATTR_SESSION_ID));
        systemParams.setRealm(requiredAttribute(webRequest, ATTR_REALM));
        return systemParams;
    }

    private String requiredAttribute(NativeWebRequest webRequest, String name) {
        Object value = webRequest.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
        if (value == null) {
            throw new MissingSystemParamException(name);
        }
        String stringValue = value.toString();
        if (stringValue.isBlank()) {
            throw new MissingSystemParamException(name);
        }
        return stringValue;
    }
}
