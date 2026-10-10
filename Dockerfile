# Build stage: compile + test + package with Maven.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B verify

# Runtime stage: JRE only, no build tools.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/credit-simulator-*.jar app.jar
COPY file_inputs.txt ./
ENTRYPOINT ["java", "-jar", "app.jar"]
