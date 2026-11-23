# syntax=docker/dockerfile:1.7
# ============================================================================
# CampusOverflow 后端镜像（Lab 8）
#
# 多阶段：builder（JDK 21 + Maven）→ runtime（JRE 21，非 root）
# 构建（在仓库根目录）：
#   docker build -t campus-overflow/backend:latest .
# 运行：
#   docker run --rm -p 8080:8080 \
#     -e DB_URL='jdbc:mysql://host.docker.internal:3306/campus_overflow?...' \
#     -e DB_USERNAME=campus -e DB_PASSWORD=*** campus-overflow/backend:latest
# ============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 先只拷 POM，利用 Docker 层缓存把依赖下载与源码变更解耦
COPY pom.xml .
COPY co-shared-kernel/pom.xml co-shared-kernel/pom.xml
COPY co-identity/pom.xml co-identity/pom.xml
COPY co-qa/pom.xml co-qa/pom.xml
COPY co-reputation/pom.xml co-reputation/pom.xml
COPY co-discovery/pom.xml co-discovery/pom.xml
COPY co-bootstrap/pom.xml co-bootstrap/pom.xml
COPY co-coverage/pom.xml co-coverage/pom.xml
RUN mvn -B -q -DskipTests dependency:go-offline || true

COPY . .
# 只构建运行所需模块（不含 Lab 7 的 co-coverage 聚合模块）
RUN mvn -B -DskipTests -pl co-bootstrap -am clean package

# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# 以非 root 运行（Lab 8 Step 1 要求）
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /workspace/co-bootstrap/target/campus-overflow.jar /app/app.jar
RUN chown -R app:app /app

USER app
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom" \
    TZ=Asia/Shanghai

HEALTHCHECK --interval=30s --timeout=3s --start-period=50s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
