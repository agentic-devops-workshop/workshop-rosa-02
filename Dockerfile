# =========================================================================
# Dockerfile — SIFAP 2.0 Monolith (backend Java 21 + Spring Boot 3.3)
# Build em duas etapas: Maven cache → runtime JRE 21 slim.
# =========================================================================

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Cache de dependências Maven
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    apt-get update && apt-get install -y --no-install-recommends maven \
    && mvn -B -q dependency:go-offline

# Build da aplicação
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests package \
    && cp target/sifap-monolith-*.jar /workspace/app.jar

# -------------------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN useradd --system --uid 1001 sifap
USER sifap

COPY --from=build /workspace/app.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
