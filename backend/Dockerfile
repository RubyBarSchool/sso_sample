FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/src/main/resources/application-example.yaml /app/application.yaml
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.config.location=file:/app/application.yaml", "app.jar"]

