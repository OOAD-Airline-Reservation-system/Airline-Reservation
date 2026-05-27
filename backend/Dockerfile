# Stage 1: Maven Build
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies for offline caching
RUN mvn dependency:go-offline
COPY src ./src
# Build the jar file, skipping tests to speed up the build
RUN mvn clean package -DskipTests

# Stage 2: Java Runtime
FROM openjdk:17-jdk-slim
WORKDIR /app
# Copy the compiled jar from Stage 1
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
