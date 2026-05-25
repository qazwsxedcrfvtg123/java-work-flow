FROM eclipse-temurin:11-jre-alpine

WORKDIR /app

COPY target/auth-module-*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
