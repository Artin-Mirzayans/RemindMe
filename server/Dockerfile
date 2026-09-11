# syntax=docker/dockerfile:1

# --- build -----------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Warm the dependency cache in its own layer. `|| true` because dependency:go-offline
# occasionally can't pre-resolve every plugin; the package step below re-resolves what's
# missing, and a genuinely broken pom still fails there.
COPY pom.xml .
RUN mvn -B dependency:go-offline -DskipTests || true

COPY src ./src
# Tests already ran in CI before this image is built.
RUN mvn -B clean package -DskipTests

# --- run -----------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /build/target/remindme-*.jar app.jar

EXPOSE 8080

# SerialGC + a RAM cap keep the JVM comfortable on a 1 GB instance.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
