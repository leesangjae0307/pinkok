# ---- 1단계: 빌드 ----
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
# 소스보다 먼저 의존성을 받아 두면, 소스만 바뀐 빌드에서 캐시를 쓴다
RUN ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ---- 2단계: 실행 ----
FROM eclipse-temurin:17-jre-jammy
# PinLog 영상 합성에 FFmpeg 가 필요하다
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
# Cloud Run 은 PORT 환경변수로 포트를 알려 준다 (없으면 8080)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java -jar app.jar --server.port=${PORT:-8080}"]
