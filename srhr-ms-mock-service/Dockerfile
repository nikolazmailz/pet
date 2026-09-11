ARG BASE_IMAGE=eclipse-temurin:17-jre
FROM ${BASE_IMAGE}

WORKDIR /app

COPY target/srhr-ms-mock-service.jar /app/srhr-ms-mock-service.jar

EXPOSE 8080
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/srhr-ms-mock-service.jar"]
