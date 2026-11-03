FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S suporte && adduser -S suporte -G suporte
COPY --from=build /app/target/suporte-0.1.0.jar app.jar
USER suporte
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
