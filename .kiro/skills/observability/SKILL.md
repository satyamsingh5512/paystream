---
name: observability
description: >
  Set up observability: Prometheus custom metrics, structured JSON logging, Correlation ID,
  Grafana dashboards, and health check configuration for PayStream services.
---

# Observability Setup Guide

## 1. Custom Metrics
```java
@Component
public class PaymentMetrics {
    private final Counter transactionsTotal;
    private final Counter transactionsFailed;
    private final Timer transactionDuration;

    public PaymentMetrics(MeterRegistry registry) {
        this.transactionsTotal = Counter.builder("paystream.transactions.total")
            .tag("service", "transaction-service").register(registry);
        this.transactionsFailed = Counter.builder("paystream.transactions.failed").register(registry);
        this.transactionDuration = Timer.builder("paystream.transaction.duration")
            .publishPercentiles(0.5, 0.95, 0.99).register(registry);
    }

    public void recordSuccess(Duration d) { transactionsTotal.increment(); transactionDuration.record(d); }
    public void recordFailure() { transactionsTotal.increment(); transactionsFailed.increment(); }
}
```

## 2. Structured JSON Logging (logback-spring.xml)
```xml
<configuration>
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <customFields>{"service":"${spring.application.name}"}</customFields>
      <includeMdcKeyName>traceId</includeMdcKeyName>
      <includeMdcKeyName>correlationId</includeMdcKeyName>
      <includeMdcKeyName>userId</includeMdcKeyName>
    </encoder>
  </appender>
  <root level="INFO"><appender-ref ref="JSON"/></root>
</configuration>
```

## 3. Correlation ID Filter
```java
@Component @Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        String id = Optional.ofNullable(req.getHeader("X-Correlation-ID"))
            .orElse(UUID.randomUUID().toString());
        MDC.put("correlationId", id);
        res.setHeader("X-Correlation-ID", id);
        try { chain.doFilter(req, res); } finally { MDC.clear(); }
    }
}
```

## 4. Prometheus Scrape Config
```yaml
global:
  scrape_interval: 15s
scrape_configs:
  - job_name: 'paystream-services'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'auth-service:8081'
          - 'account-service:8082'
          - 'transaction-service:8083'
          - 'fraud-service:8084'
          - 'notification-service:8085'
          - 'ledger-service:8086'
```

## 5. Key Grafana Panels
1. **Transaction Rate** — `rate(paystream_transactions_total[5m])`
2. **Error Rate %** — `rate(paystream_transactions_failed[5m]) / rate(paystream_transactions_total[5m]) * 100`
3. **P95 Latency** — `histogram_quantile(0.95, rate(paystream_transaction_duration_seconds_bucket[5m]))`
4. **Circuit Breaker States** — `resilience4j_circuitbreaker_state`
5. **Fraud Alert Rate** — `rate(paystream_fraud_alerts_total[5m])`
6. **Kafka Consumer Lag** — `kafka_consumer_fetch_manager_records_lag_max`
7. **JVM Heap per Service** — `jvm_memory_used_bytes{area="heap"}`
8. **Active WebSocket Sessions** — custom gauge from notification-service
