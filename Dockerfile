# =============================================================
# Stage 1 — Build
# =============================================================
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

# Cache dependencies first
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd ./
RUN mvn -B -q dependency:go-offline

# Copy sources and build
COPY src ./src
RUN mvn -B -q clean package -DskipTests \
    && cp target/payments-core-api-*.jar /workspace/app.jar

# =============================================================
# Stage 2 — Runtime
# =============================================================
FROM eclipse-temurin:25-jre-alpine

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0" \
    SERVER_PORT=8080

WORKDIR /app

# Run as non-root
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /workspace/app.jar /app/app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:${SERVER_PORT}/api/v1/health || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
