# 使用官方 OpenJDK 8 镜像
FROM openjdk:8-jdk-alpine

# 设置工作目录
WORKDIR /app

# 安装必要的工具
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 复制 Maven 构建文件
COPY pom.xml .
COPY src ./src

# 复制 Maven wrapper
COPY .mvn ./.mvn
COPY mvnw .

# 赋予执行权限并构建应用
RUN chmod +x mvnw && \
    ./mvnw clean package -DskipTests

# 创建应用用户
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

# 创建上传目录
RUN mkdir -p /app/uploads && \
    chown -R appuser:appuser /app

# 切换到应用用户
USER appuser

# 暴露端口
EXPOSE 5678

# 设置 JVM 参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"

# 启动应用
CMD ["sh", "-c", "java $JAVA_OPTS -jar target/*.jar"]

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:5678/actuator/health || exit 1
