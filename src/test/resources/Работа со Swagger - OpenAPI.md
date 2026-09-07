# Работа со Swagger / OpenAPI

При изменении API-контракта сервиса необходимо также обновить его Swagger/OpenAPI-описание.

## Обновление Swagger

Если в сервисе изменяется контракт API, необходимо:

1. Сформировать актуальный OpenAPI YAML-файл.
2. Перейти в проект `srhr-swagger`.
3. Добавить новый либо обновить существующий файл:

```text
openapi_<service-name>.yaml
```

Например:

```text
openapi_paystub.yaml
```

Файл в `srhr-swagger` должен соответствовать актуальному контракту сервиса.

## Генерация OpenAPI YAML

Один из вариантов формирования актуального YAML — генерация через интеграционный тест.

<details>
<summary>Пример интеграционного теста для генерации OpenAPI YAML</summary>

```java
@Test
void shouldGenerateOpenApiYaml() throws Exception {
    byte[] openApiYaml = mockMvc.perform(get("/v3/api-docs.yaml"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    Path outputFile = Path.of(
            "build",
            "generated",
            "openapi",
            "openapi.yaml"
    );

    Files.createDirectories(outputFile.getParent());
    Files.write(outputFile, openApiYaml);
}
```

</details>

Тест получает актуальное OpenAPI-описание через endpoint:

```text
/v3/api-docs.yaml
```

После выполнения теста сгенерированный файл можно найти по пути:

```text
build/generated/openapi/openapi.yaml
```

Полученный `openapi.yaml` необходимо перенести в проект `srhr-swagger` и сохранить под именем соответствующего сервиса:

```text
openapi_<service-name>.yaml
```

Если файл для данного сервиса уже существует, его необходимо обновить.

## Итоговый процесс

```text
Изменение контракта
        ↓
Генерация актуального openapi.yaml
        ↓
Проверка сгенерированного файла
        ↓
Обновление openapi_<service-name>.yaml
в проекте srhr-swagger
        ↓
Commit изменений контракта и Swagger
```

Изменение API-контракта без соответствующего обновления Swagger считается незавершённым изменением.