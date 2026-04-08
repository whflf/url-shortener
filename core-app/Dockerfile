# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/
COPY core-app/pom.xml core-app/
COPY analytics/pom.xml analytics/
COPY common/src common/src
COPY core-app/src core-app/src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/core-app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]