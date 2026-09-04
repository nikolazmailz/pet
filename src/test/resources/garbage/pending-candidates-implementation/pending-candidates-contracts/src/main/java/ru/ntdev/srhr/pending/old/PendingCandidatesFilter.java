@Schema(description = "Фильтры списка кандидатов, ожидающих действия")
public record PendingCandidatesFilter(

        @Schema(description = "Строка поиска", maxLength = 255, nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                example = "Иванов")
        @Size(max = 255)
        String search,

        @ArraySchema(
                schema = @Schema(description = "Код этапа", maxLength = 128, example = "interview"),
                maxItems = 100,
                arraySchema = @Schema(description = "Коды этапов", nullable = true))
        @Size(max = 100)
        List<@Size(max = 128) String> eventCodeList
) {}