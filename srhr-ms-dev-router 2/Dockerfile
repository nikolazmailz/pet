ARG RUNTIME_IMAGE=eclipse-temurin:17-jre
FROM ${RUNTIME_IMAGE}

WORKDIR /opt/app
COPY target/srhr-ms-dev-router-*.jar app.jar

USER 10001
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/opt/app/app.jar"]
