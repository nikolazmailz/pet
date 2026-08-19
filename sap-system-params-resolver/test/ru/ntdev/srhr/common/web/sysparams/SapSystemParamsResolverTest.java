package ru.ntdev.srhr.common.web.sysparams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import ru.ntdev.srhr.common.dto.SapSystemParamsDto;

class SapSystemParamsResolverTest {

    private final SapSystemParamsResolver resolver = new SapSystemParamsResolver();

    private MockHttpServletRequest request;
    private NativeWebRequest webRequest;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        webRequest = new ServletWebRequest(request);
    }

    // --- supportsParameter ---

    @Test
    void supportsAnnotatedDtoParameter() {
        assertThat(resolver.supportsParameter(param("annotated", SapSystemParamsDto.class))).isTrue();
    }

    @Test
    void doesNotSupportDtoWithoutAnnotation() {
        assertThat(resolver.supportsParameter(param("notAnnotated", SapSystemParamsDto.class))).isFalse();
    }

    @Test
    void doesNotSupportAnnotatedParameterOfWrongType() {
        assertThat(resolver.supportsParameter(param("wrongType", String.class))).isFalse();
    }

    // --- resolveArgument ---

    @Test
    void resolvesAllFieldsFromRequestAttributes() throws Exception {
        setAllAttributes();

        SapSystemParamsDto dto = (SapSystemParamsDto) resolver.resolveArgument(
                param("annotated", SapSystemParamsDto.class), null, webRequest, null);

        assertThat(dto.getChannel()).isEqualTo("WEB");
        assertThat(dto.getAdLogin()).isEqualTo("i.ivanov");
        assertThat(dto.getSessionId()).isEqualTo("sess-42");
        assertThat(dto.getRealm()).isEqualTo("internal");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            SapSystemParamsResolver.ATTR_CHANNEL,
            SapSystemParamsResolver.ATTR_AD_LOGIN,
            SapSystemParamsResolver.ATTR_SESSION_ID,
            SapSystemParamsResolver.ATTR_REALM
    })
    void throwsWhenRequiredAttributeMissing(String missing) {
        setAllAttributes();
        request.removeAttribute(missing);

        assertThatThrownBy(() -> resolver.resolveArgument(
                param("annotated", SapSystemParamsDto.class), null, webRequest, null))
                .isInstanceOf(MissingSystemParamException.class)
                .extracting(e -> ((MissingSystemParamException) e).getParamName())
                .isEqualTo(missing);
    }

    @Test
    void throwsWhenAttributeIsBlank() {
        setAllAttributes();
        request.setAttribute(SapSystemParamsResolver.ATTR_REALM, "   ");

        assertThatThrownBy(() -> resolver.resolveArgument(
                param("annotated", SapSystemParamsDto.class), null, webRequest, null))
                .isInstanceOf(MissingSystemParamException.class);
    }

    // --- helpers ---

    private void setAllAttributes() {
        request.setAttribute(SapSystemParamsResolver.ATTR_CHANNEL, "WEB");
        request.setAttribute(SapSystemParamsResolver.ATTR_AD_LOGIN, "i.ivanov");
        request.setAttribute(SapSystemParamsResolver.ATTR_SESSION_ID, "sess-42");
        request.setAttribute(SapSystemParamsResolver.ATTR_REALM, "internal");
    }

    private MethodParameter param(String methodName, Class<?> type) {
        try {
            Method method = TestMethods.class.getDeclaredMethod(methodName, type);
            return new MethodParameter(method, 0);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unused")
    private static class TestMethods {
        void annotated(@SapSystemParams SapSystemParamsDto dto) { }

        void notAnnotated(SapSystemParamsDto dto) { }

        void wrongType(@SapSystemParams String value) { }
    }
}
