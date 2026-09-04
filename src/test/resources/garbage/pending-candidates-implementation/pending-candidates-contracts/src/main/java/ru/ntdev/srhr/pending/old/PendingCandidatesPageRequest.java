@Schema(description = "Запрос страницы кандидатов, ожидающих действия")
public record PendingCandidatesPageRequest(

        @Schema(description = "Номер страницы", minimum = "1", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(1)
        int page,

        @Schema(description = "Размер страницы", minimum = "1", maximum = "200", example = "50",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(1) @Max(200)
        int pageSize,

        @Schema(description = "Фильтры", nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        PendingCandidatesFilter filter
) {}