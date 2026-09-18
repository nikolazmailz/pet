# srhr-ms-dev-gw

DEV gateway на Spring Cloud Gateway.

Он принимает запросы, выбирает DEV-пользователя, добавляет его JWT и
перенаправляет запрос во внутренний SRHR-сервис. WireMock EPA пока не подключён.

Дополнительно настроены таймауты исходящих HTTP-запросов, Actuator health-check
и журналирование метода, пути, маршрута, статуса и времени выполнения запроса.
Также включён CORS для DEV-микрофронтов и локальной разработки.

## DEV JWT

Gateway создаёт новый ES256 JWT для каждого проксируемого запроса. Входящий
заголовок `Authorization` удаляется и заменяется на:

```text
Authorization: Bearer <DEV JWT>
```

В токен добавляются `sub`, `ctxi`, `channel`, `realm`, `ip`, `jti`, `iss`,
`aud`, `iat`, `nbf` и `exp`.

В `application.yml` настроены профили `employee`, `approver` и `admin`.
Приоритет выбора пользователя:

1. Заголовок `X-Dev-User`.
2. Cookie `DEV_USER`.
3. Значение `dev-gateway.jwt.default-user`.

Для выбора через браузер откройте:

```text
http://localhost:8090/dev-users.html
```

Страница сохраняет HttpOnly-cookie `DEV_USER` и `DEV_SESSION_ID`. Gateway
использует идентификатор браузерной сессии как claim `ctxi`.

Для Postman или curl пользователь выбирается заголовком:

```bash
curl -H 'X-Dev-User: approver' \
  http://localhost:8090/ui-api-web/cross/srhr/paystub/v1/paystub/period
```

Параметры профилей можно переопределить переменными окружения:

```text
DEV_JWT_DEFAULT_USER
DEV_JWT_EMPLOYEE_SUB
DEV_JWT_EMPLOYEE_SESSION_ID
DEV_JWT_EMPLOYEE_CHANNEL
DEV_JWT_EMPLOYEE_REALM
DEV_JWT_APPROVER_SUB
DEV_JWT_ADMIN_SUB
DEV_JWT_ISSUER
DEV_JWT_AUDIENCE
DEV_JWT_KEY_ID
DEV_JWT_TTL
```

Публичный ключ доступен в формате JWKS:

```bash
curl http://localhost:8090/.well-known/jwks.json
```

EC-ключ создаётся при старте Gateway. После перезапуска публичный ключ
изменяется. Подключение проверки через WireMock EPA выполняется отдельным шагом.

Если frontend и Gateway работают на разных origin, frontend должен отправлять
запросы с `credentials: 'include'` или `withCredentials: true`, иначе cookie
выбранного пользователя не попадёт в Gateway и будет использован
`default-user`.

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

Проверить состояние приложения:

```bash
curl http://localhost:8090/actuator/health
curl http://localhost:8090/actuator/health/readiness
curl http://localhost:8090/actuator/health/liveness
```

Таймаут подключения по умолчанию — 3 секунды, ожидания ответа — 30 секунд.
Их можно изменить переменными `ROUTER_CONNECT_TIMEOUT_MS` и
`ROUTER_RESPONSE_TIMEOUT`.

## CORS

По умолчанию разрешены запросы от:

```text
https://*.srhr.innodev.local
http://localhost:*
```

Шаблоны можно переопределить:

```bash
export FRONTEND_ORIGIN_PATTERN=https://*.example.local
export LOCAL_FRONTEND_ORIGIN_PATTERN=http://localhost:*
```

Разрешены методы `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`, любые
заголовки и передача cookies. Результат предварительного `OPTIONS`-запроса
кэшируется браузером на один час.

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
