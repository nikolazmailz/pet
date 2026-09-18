# srhr-ms-dev-gw

Минимальный DEV gateway на Spring Cloud Gateway.

Он только принимает запросы и перенаправляет их во внутренние SRHR-сервисы.
JWT, авторизация, выбор пользователя, WireMock и изменение заголовков отсутствуют.

## Требования

- JDK 17;
- Maven 3.9 или новее.

Если зависимости загружаются через корпоративный Nexus, используйте свой
`settings.xml`:

```bash
mvn clean package -s /path/to/settings.xml
```

## Запуск

```bash
mvn spring-boot:run
```

Gateway запустится на порту `8090`.

Адреса сервисов по умолчанию:

- `userinfo` — `http://localhost:8081`;
- `paystub` — `http://localhost:8082`;
- `userphoto` — `http://localhost:8083`.

Их можно изменить переменными окружения:

```bash
export USERINFO_SERVICE_URI=http://srhr-ms-userinfo-rest:8180
export PAYSTUB_SERVICE_URI=http://srhr-ms-paystub-rest:8180
export USERPHOTO_SERVICE_URI=http://srhr-ms-userphoto-rest:8180
mvn spring-boot:run
```

## Маршруты

```text
/ui-api-web/cross/srhr/userinfo/** -> USERINFO_SERVICE_URI/**
/ui-api-web/cross/srhr/paystub/**  -> PAYSTUB_SERVICE_URI/**
/ui-api-web/cross/srhr/userphoto/** -> USERPHOTO_SERVICE_URI/**
```

`StripPrefix=4` удаляет части `/ui-api-web/cross/srhr/{service}` и передаёт
внутреннему сервису оставшийся путь.

Пример:

```text
GET http://localhost:8090/ui-api-web/cross/srhr/paystub/v1/paystub/period
  -> http://localhost:8082/v1/paystub/period
```
