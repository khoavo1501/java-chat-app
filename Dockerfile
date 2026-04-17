FROM maven:3.9.9-eclipse-temurin-11 AS builder

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:11-jre

LABEL maintainer="MiniMax Agent"
LABEL description="Java Chat App Spring Boot with MongoDB"
LABEL version="2.0.0"

WORKDIR /app

COPY --from=builder /workspace/target/java-chat-app-2.0.0.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD ["sh", "-c", "echo > /dev/tcp/127.0.0.1/8080"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
