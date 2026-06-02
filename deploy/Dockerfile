# ===== 多阶段构建: 后端 Spring Boot =====
FROM maven:3.8-openjdk-8-slim AS builder
WORKDIR /app
COPY backend/pom.xml .
# 先下载依赖（利用 Docker 缓存层）
RUN mvn dependency:go-offline -q || true
COPY backend/src ./src
RUN mvn clean package -DskipTests -q

# ===== 运行时 =====
FROM openjdk:8-jre-slim
WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p /app/uploads

EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl -f http://localhost:8081/api/health/ping || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
