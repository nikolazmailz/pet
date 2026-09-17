FROM eclipse-temurin:17-jre

WORKDIR /app
ARG JAR_FILE=target/srhr-ms-dev-router-0.1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

USER 10001
EXPOSE 8090

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
