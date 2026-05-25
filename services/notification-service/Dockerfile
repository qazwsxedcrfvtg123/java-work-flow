FROM eclipse-temurin:11-jre-alpine

WORKDIR /app

COPY target/notification-service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
