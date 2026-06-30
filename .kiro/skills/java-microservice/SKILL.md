---
name: java-microservice
description: >
  Scaffold a new Spring Boot 3 microservice for PayStream with all standard config:
  Maven module, Flyway migrations, Actuator, Prometheus metrics, Resilience4j,
  Eureka registration, OpenAPI docs, Dockerfile, and base Testcontainers integration test.
---

# Java Microservice Scaffolder

## Maven Structure
```
<service-name>/
├── pom.xml
└── src/
    ├── main/java/com/paystream/<service-name>/
    │   ├── <ServiceName>Application.java
    │   ├── config/
    │   ├── controller/
    │   ├── service/
    │   ├── repository/
    │   ├── domain/
    │   ├── dto/
    │   ├── event/
    │   └── exception/GlobalExceptionHandler.java
    └── test/java/com/paystream/<service-name>/
        └── <ServiceName>IntegrationTest.java
```

## Required Dependencies (pom.xml)
- spring-boot-starter-web
- spring-boot-starter-actuator
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- spring-cloud-starter-netflix-eureka-client
- spring-cloud-starter-config
- micrometer-registry-prometheus
- resilience4j-spring-boot3
- springdoc-openapi-starter-webmvc-ui
- flyway-core
- postgresql (runtime)
- testcontainers-bom (test)

## application.yml Template
```yaml
spring:
  application:
    name: <service-name>
  config:
    import: "optional:configserver:http://config-server:8888"
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/<service>_db
    username: ${DB_USER:paystream}
    password: ${DB_PASS:paystream}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  threads:
    virtual:
      enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      probes:
        enabled: true

eureka:
  client:
    service-url:
      defaultZone: http://${EUREKA_HOST:localhost}:8761/eureka/

resilience4j:
  circuitbreaker:
    instances:
      default:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        sliding-window-size: 10
  retry:
    instances:
      default:
        max-attempts: 3
        wait-duration: 500ms
        enable-exponential-backoff: true
```

## Dockerfile Template
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S paystream && adduser -S paystream -G paystream
COPY --from=builder /app/target/*.jar app.jar
USER paystream
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "app.jar"]
```

## Integration Test Template
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class <ServiceName>IntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }
}
```

## GlobalExceptionHandler Template
@RestControllerAdvice handling:
- EntityNotFoundException → 404
- OptimisticLockException → 409 with retry message
- MethodArgumentNotValidException → 400 with field errors list
- Exception (fallback) → 500 with correlationId from MDC
