# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/
COPY core-app/pom.xml core-app/
COPY analytics/pom.xml analytics/
COPY common/src common/src
COPY analytics/src analytics/src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/analytics/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]