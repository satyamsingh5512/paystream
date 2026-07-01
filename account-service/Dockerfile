# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Copy all module poms so Maven can build the full reactor model declared in the parent pom
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY account-service/pom.xml account-service/pom.xml
COPY transaction-service/pom.xml transaction-service/pom.xml
COPY fraud-service/pom.xml fraud-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ledger-service/pom.xml ledger-service/pom.xml
RUN ./mvnw -q -pl account-service -am dependency:go-offline
COPY account-service/src account-service/src
RUN ./mvnw -q -pl account-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S paystream && adduser -S paystream -G paystream
COPY --from=builder /app/account-service/target/account-service.jar app.jar
USER paystream
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
  CMD wget -q --spider http://localhost:8082/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "app.jar"]
