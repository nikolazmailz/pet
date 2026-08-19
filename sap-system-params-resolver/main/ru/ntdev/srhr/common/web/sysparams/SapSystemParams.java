package ru.ntdev.srhr.common.web.sysparams;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает аргумент метода контроллера типа {@code SapSystemParamsDto},
 * который должен быть заполнен из атрибутов текущего HTTP-запроса
 * (устанавливаются {@code JwtTokenFilter}).
 *
 * <p>Пример:
 * <pre>{@code
 * @PostMapping("/comment")
 * ResponseEntity<...> addComment(@RequestBody RequisitionAddCommentRequest body,
 *                                @SapSystemParams SapSystemParamsDto systemParams) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SapSystemParams {
}
