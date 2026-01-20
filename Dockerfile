# 1단계: 빌드 스테이지 (Gradle 빌드)
# OCI ARM64 환경에 맞춰 이미지를 빌드하기 위해 멀티 스테이지 빌드를 사용합니다.
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Gradle 래퍼와 설정 파일들을 먼저 복사하여 종속성 캐싱을 최적화합니다.
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle

# 소스 코드를 복사합니다.
COPY src src

# 애플리케이션 빌드 (-x check -x test 옵션으로 빌드 속도를 높입니다)
RUN ./gradlew clean build -x check -x test --no-daemon

# 2단계: 실행 스테이지 (가벼운 JRE 환경)
# eclipse-temurin:17-jre-alpine 이미지는 용량이 매우 작아 OCI 프리티어의 저장 공간과 네트워크 대역폭을 절약합니다.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 빌드 스테이지에서 생성된 jar 파일만 가져옵니다.
COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너 내부 8080 포트를 노출합니다.
EXPOSE 8080

# 애플리케이션 실행
# JVM 옵션은 GitHub Actions 배포 스크립트에서 동적으로 주입할 예정입니다.
ENTRYPOINT ["java", "-jar", "app.jar"]
