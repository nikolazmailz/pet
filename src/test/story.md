## Servlet filter
Изменить SapSystemParamsDto в контроллерах
Реализовать при помощи фильтра

```java

@Component
public class SapRequestContextFilter extends OncePerRequestFilter {

    public static final String SAP_REQUEST_CONTEXT =
            SapRequestContext.class.getName();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        SapRequestContext context = new SapRequestContext(
                getRequiredAttribute(request, "channel"),
                getRequiredAttribute(request, "adLogin"),
                getRequiredAttribute(request, "sessionId"),
                getRequiredAttribute(request, "realm")
        );

        request.setAttribute(SAP_REQUEST_CONTEXT, context);

        filterChain.doFilter(request, response);
    }

    private String getRequiredAttribute(
            HttpServletRequest request,
            String name
    ) {
        Object value = request.getAttribute(name);

        if (value == null) {
            throw new IllegalStateException(
                    "Отсутствует атрибут запроса: " + name
            );
        }

        return value.toString();
    }
}

public record SapRequestContext(
        String channel,
        String adLogin,
        String sessionId,
        String realm
) {
}
```


Контроллер перегружен технической логикой

Сейчас он:

устанавливает request attribute;
собирает системные параметры;
вызывает сервис;
логирует интеграционный ответ;
интерпретирует код внешней системы;
маппит DTO;
очищает DTO;
формирует HTTP-ошибку.

Контроллер в идеале должен выглядеть приблизительно так:

```java
@PostMapping
public ResponseEntity<RequisitionGeneralCandidateDtoOut> addComment(
        @Valid @RequestBody RequisitionAddCommentRequest requestDto,
        SapSystemParamsDto systemParams
) {
    RequisitionGeneralCandidateDtoOut response =
            requisitionService.addComment(systemParams, requestDto);

    return ResponseEntity.ok(response);
}
```



