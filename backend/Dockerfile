# syntax=docker/dockerfile:1

# ---------- Build stage: compile with the project's own Maven wrapper ----------
FROM eclipse-temurin:17.0.20_8-jdk-noble AS build
WORKDIR /build

# Dependencies first: this layer is rebuilt only when pom.xml changes, not on every
# source edit. The cache mount keeps downloaded jars between builds.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests need MySQL and run in CI / locally, not inside the image build.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && cp target/*.jar app.jar \
    # Split the fat jar into layers (dependencies change rarely, our code often), so a
    # code change re-uploads only a small layer.
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---------- Runtime stage: JRE only, no compiler or Maven ----------
FROM eclipse-temurin:17.0.20_8-jre-noble

# Install the latest Ubuntu security updates on top of the pinned base image (the base
# tag is fixed, its OS packages are not frozen in time), plus curl for the health check.
RUN apt-get update \
    && apt-get upgrade -y --no-install-recommends \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system placementhub \
    && useradd --system --gid placementhub --no-create-home --shell /usr/sbin/nologin placementhub

WORKDIR /app
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./

# Never run as root inside the container.
USER placementhub

# Size the heap from the container's memory limit; crash fast (and get restarted)
# instead of limping on after an OutOfMemoryError.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Liveness: is the JVM serving requests at all? (Readiness, which also checks MySQL, is
# used by docker compose to decide when the frontend may start.)
HEALTHCHECK --interval=15s --timeout=3s --start-period=90s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
