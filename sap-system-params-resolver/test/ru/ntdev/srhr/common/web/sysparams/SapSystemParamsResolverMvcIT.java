package ru.ntdev.srhr.common.web.sysparams;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.ntdev.srhr.common.dto.SapSystemParamsDto;

/**
 * Интеграционный тест связки MVC + резолвер: контроллер объявляет аргумент
 * {@code @SapSystemParams SapSystemParamsDto} и получает заполненный объект.
 *
 * <p>Тест уровня библиотеки. В сервисах, подключающих резолвер, дополнительно
 * нужен сквозной тест реального контроллера с {@code JwtTokenFilter}
 * (атрибуты устанавливает фильтр, а не тест).
 */
class SapSystemParamsResolverMvcIT {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setCustomArgumentResolvers(new SapSystemParamsResolver())
                .setControllerAdvice(new TestExceptionHandler())
                .build();
    }

    @Test
    void controllerReceivesResolvedSystemParams() throws Exception {
        mockMvc.perform(get("/sysparams-test")
                        .requestAttr("channel", "MOBILE")
                        .requestAttr("adLogin", "p.petrov")
                        .requestAttr("sessionId", "sess-777")
                        .requestAttr("realm", "external"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("MOBILE"))
                .andExpect(jsonPath("$.adLogin").value("p.petrov"))
                .andExpect(jsonPath("$.sessionId").value("sess-777"))
                .andExpect(jsonPath("$.realm").value("external"));
    }

    @Test
    void missingRequiredAttributeIsRejected() throws Exception {
        mockMvc.perform(get("/sysparams-test")
                        .requestAttr("channel", "MOBILE")
                        .requestAttr("adLogin", "p.petrov")
                        // sessionId отсутствует
                        .requestAttr("realm", "external"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.param").value("sessionId"));
    }

    @RestController
    static class TestController {
        @GetMapping("/sysparams-test")
        SapSystemParamsDto endpoint(@SapSystemParams SapSystemParamsDto systemParams) {
            return systemParams;
        }
    }

    @RestControllerAdvice
    static class TestExceptionHandler {
        @ExceptionHandler(MissingSystemParamException.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        ErrorBody handle(MissingSystemParamException e) {
            return new ErrorBody(e.getParamName(), e.getMessage());
        }
    }

    record ErrorBody(String param, String message) { }
}
