FROM eclipse-temurin:11-jre-alpine

WORKDIR /app

COPY target/workflow-service-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
