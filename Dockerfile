# Multi-stage build (using glibc-based Debian image for ONNX Runtime compatibility)
FROM eclipse-temurin:25-jdk@sha256:83f4a7fc27df94dcc7b8de04d14cbbba8da14406f921e966969c3b09000aa507 AS builder
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
WORKDIR /build

# Copy POM, sources, and models
COPY pom.xml .
COPY src ./src
COPY models ./models

RUN mvn clean package -DskipTests

# Runtime stage (glibc-based Debian image for ONNX Runtime native lib compatibility)
FROM eclipse-temurin:25-jre@sha256:ecdc0adb37bde1a281c4d245a3cd535004b1fc8322d560f5b09463b248c81df0

# Baseline OCI metadata so a plain `docker build` or `docker compose build` also
# produces a described image. The CD workflow overrides the dynamic fields
# (version, revision, created) and copies them onto the manifest index too.
LABEL org.opencontainers.image.title="Dice Chess Java Bot" \
      org.opencontainers.image.description="Official Java 25 reference house bot and template for the Dice Chess platform. Powered by ONNX Runtime and the Scala 3 engine." \
      org.opencontainers.image.url="https://github.com/fortemate/dicechess-bot-java" \
      org.opencontainers.image.source="https://github.com/fortemate/dicechess-bot-java" \
      org.opencontainers.image.documentation="https://bots.jc.id.lv" \
      org.opencontainers.image.vendor="Fortemate" \
      org.opencontainers.image.licenses="AGPL-3.0-only" \
      org.opencontainers.image.base.name="docker.io/library/eclipse-temurin:25-jre"

WORKDIR /app

ENV JAVA_OPTS="-Xmx256m --enable-native-access=ALL-UNNAMED -XX:+UseG1GC"
ENV PORT=8080
ENV MODEL_PATH="/app/models/baseline.onnx"

COPY --from=builder /build/target/dicechess-bot-java-*.jar /app/app.jar
COPY --from=builder /build/models /app/models

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
