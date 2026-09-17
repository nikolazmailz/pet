# srhr-ms-dev-router

DEV-only reverse proxy for SRHR services. It replaces the small part of corporate Tyk behavior
that is needed on an isolated development stand:

- routes public UI paths to real SRHR services;
- selects a predefined test user using `X-Dev-User` or the `DEV_USER` cookie;
- removes untrusted identity headers supplied by the client;
- injects the headers configured for the selected user;
- generates `sessionId`, `traceId`, and `sessionCreatedAt` for every proxied request;
- provides a small API for listing and selecting test users.

This application does **not** emulate Tyk Dashboard, MDCB, Pump, rate limits, analytics, or MAPI.
External integrations used by SRHR services should be routed separately to `srhr-ms-mock-service`.

## Requirements

- JDK 17+
- Maven 3.9+

## Run locally

```shell
./mvnw clean verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

On Windows Command Prompt use `mvnw.cmd`. If Maven Central is unavailable, set
`MAVEN_DISTRIBUTION_URL` to the approved corporate mirror URL for
`apache-maven-3.9.9-bin.zip`.

By default, the router starts at `http://localhost:8090` and expects:

| Service | Default URI | Environment variable |
|---|---|---|
| User info | `http://localhost:8081` | `USERINFO_SERVICE_URI` |
| Paystub | `http://localhost:8082` | `PAYSTUB_SERVICE_URI` |

Example with explicit upstreams:

```shell
USERINFO_SERVICE_URI=http://localhost:8181 \
PAYSTUB_SERVICE_URI=http://localhost:8182 \
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Select a user

List configured users:

```shell
curl http://localhost:8090/dev/session/users
```

Use one profile for a single Postman/curl request:

```shell
curl -H 'X-Dev-User: approver' \
  http://localhost:8090/ui-api-web/cross/srhr/paystub/v1/documents
```

Select a browser user by cookie:

```shell
curl -i -X POST http://localhost:8090/dev/session/user/approver
```

The response contains:

```http
Set-Cookie: DEV_USER=approver; Path=/; Max-Age=28800; HttpOnly; SameSite=Lax
```

Or open the built-in selector page:

```text
http://localhost:8090/dev-users.html
```

Selecting a user also creates an HttpOnly `DEV_SESSION_ID` cookie. The same `sessionId` is then used
for subsequent browser requests. For HTTPS stands, set `DEV_COOKIE_SECURE=true`.

Header selection has priority over cookie selection. If neither is present, `default-user` is used.

## DEV session API

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/dev/session/users` | List available profiles without exposing their headers |
| `GET` | `/dev/session/current` | Show the current profile and selection source |
| `POST` | `/dev/session/user/{id}` | Select a profile and set an HttpOnly cookie |
| `DELETE` | `/dev/session` | Clear the selection cookie |

## Configure users

Profiles are defined in `src/main/resources/application-dev.yml`:

```yaml
dev-router:
  impersonation:
    enabled: true
    default-user: employee

  users:
    approver:
      display-name: "Иванов Иван — согласующий"
      headers:
        adLogin: ivanov_ii
        pernr: "12345678"
        channel: WEB
        realm: EMPLOYEE
        roles: EMPLOYEE,APPROVER
```

Header names are deliberately configuration-driven. Replace the example names after comparing an
actual request before and after the corporate Tyk gateway.

## Security behavior

The router treats all headers used in any configured profile as protected. Before proxying, it:

1. removes protected headers received from the client;
2. removes `X-Dev-User`;
3. inserts headers from the selected server-side profile;
4. generates session and trace identifiers.

An unknown profile returns `400 Bad Request` and is not forwarded upstream.

The impersonation filter and session endpoints require both:

- the Spring profile `dev`;
- `dev-router.impersonation.enabled=true`.

Do not expose this application on a production contour.

## Add a route

Routes are configured in `application.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: requisition
          uri: "${REQUISITION_SERVICE_URI:http://localhost:8083}"
          predicates:
            - Path=/ui-api-web/cross/srhr/requisition/v1/**
          filters:
            - StripPrefix=5
```

`StripPrefix=5` converts:

```text
/ui-api-web/cross/srhr/requisition/v1/search -> /search
```

Remove or change this filter if the service expects the full external path.

## Build an image

```shell
./mvnw clean package
docker build -t srhr-ms-dev-router:0.1.0 .
```

The provided Dockerfile uses a public base image as an example. Replace it with the approved JRE 17
image from the corporate registry.

Kubernetes example: `deploy/k8s/deployment.yaml`.

## Where to mock external responses

Keep the separation clear:

```text
Frontend -> srhr-ms-dev-router -> real SRHR service -> srhr-ms-mock-service
```

The router emulates gateway identity/routing. `srhr-ms-mock-service` emulates EASUP, EPA, TSRM, and
other external integrations, including delays and error responses.
