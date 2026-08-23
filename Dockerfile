FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
RUN mvn -B -DskipTests dependency:go-offline

COPY src src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S protoseal && adduser -S protoseal -G protoseal
COPY --from=builder --chown=protoseal:protoseal /workspace/target/*.jar app.jar

USER protoseal
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
