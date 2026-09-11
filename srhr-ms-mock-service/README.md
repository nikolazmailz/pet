# srhr-ms-mock-service

Spring Boot wrapper around embedded WireMock for mocking external REST integrations on the SRHR DEV environment.

## Architecture

Two HTTP ports are exposed:

- `8080` — WireMock mock endpoints and WireMock Admin API.
- `8081` — Spring Boot Actuator.

```text
SRHR services
     |
     | REST
     v
srhr-ms-mock-service:8080
     |
     +-- TSRM mocks
     +-- EASUP mocks
     +-- FileStorage mocks
     +-- other REST integrations

Actuator:
srhr-ms-mock-service:8081/actuator/health
```

## Requirements

- Java 17
- Maven
- Maven artifact `org.wiremock:wiremock-standalone:3.13.2` must be available in the corporate Maven repository.

## Project structure

```text
src/main/resources/wiremock/
├── mappings/
│   ├── tsrm/
│   ├── easup/
│   └── ...
└── __files/
    ├── tsrm/
    ├── easup/
    └── ...
```

Mappings and response bodies are packaged into the Spring Boot JAR.

## Build

```bash
mvn clean package
```

Result:

```text
target/srhr-ms-mock-service.jar
```

## Run locally

```bash
java -jar target/srhr-ms-mock-service.jar
```

WireMock:

```text
http://localhost:8080
```

Spring Boot Actuator:

```text
http://localhost:8081/actuator/health
```

## Test TSRM stub

```bash
curl http://localhost:8080/functional-role/test-user/WEB
```

Expected response:

```json
{
  "roles": [
    {
      "roleId": "admin",
      "attributeIds": ["attr1", "attr2", "attr3"]
    },
    {
      "roleId": "user"
    }
  ]
}
```

## WireMock Admin API

Loaded mappings:

```bash
curl http://localhost:8080/__admin/mappings
```

Received requests:

```bash
curl http://localhost:8080/__admin/requests
```

Reset request journal:

```bash
curl -X DELETE http://localhost:8080/__admin/requests
```

## Add a mock

Create a mapping:

```text
src/main/resources/wiremock/mappings/<system>/<operation>.json
```

Create its response:

```text
src/main/resources/wiremock/__files/<system>/<operation>-response.json
```

Example:

```json
{
  "request": {
    "method": "GET",
    "urlPathPattern": "/functional-role/[^/]+/[^/]+"
  },
  "response": {
    "status": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "bodyFileName": "tsrm/get-role-response.json"
  }
}
```

## Configuration

Environment variables:

| Variable | Default | Purpose |
|---|---:|---|
| `WIREMOCK_PORT` | `8080` | WireMock HTTP port |
| `SERVER_PORT` | `8081` | Spring Boot / Actuator port |
| `WIREMOCK_ROOT_DIRECTORY` | `wiremock` | Classpath root for mappings and response files |
| `WIREMOCK_VERBOSE` | `true` | Verbose WireMock logging |

## Docker

Build the JAR first:

```bash
mvn clean package
```

Then build the image:

```bash
docker build -t srhr-ms-mock-service:local .
```

If a corporate JRE image must be used:

```bash
docker build \
  --build-arg BASE_IMAGE=<corporate-registry>/<approved-java17-image> \
  -t srhr-ms-mock-service:local .
```

Run:

```bash
docker run --rm \
  -p 8080:8080 \
  -p 8081:8081 \
  --name srhr-ms-mock-service \
  srhr-ms-mock-service:local
```

Or:

```bash
docker compose up -d --build
```

## Health checks

Full health:

```bash
curl http://localhost:8081/actuator/health
```

Kubernetes liveness:

```text
GET :8081/actuator/health/liveness
```

Kubernetes readiness:

```text
GET :8081/actuator/health/readiness
```

The custom `WireMockHealthIndicator` also verifies that embedded WireMock is running.

## DEV usage

External integration URLs should point to this service, for example:

```text
TSRM_INTEGRATION_SERVICE_URL=http://srhr-ms-mock-service:8080
EASUP_INTEGRATION_SERVICE_URL=http://srhr-ms-mock-service:8080
```

Kafka mocking is intentionally outside the current scope.
