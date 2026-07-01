package com.paystream.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binding for {@code paystream.account.*}. */
@ConfigurationProperties(prefix = "paystream.account")
public record AccountProperties(OptimisticLock optimisticLock, Outbox outbox) {

    public AccountProperties {
        if (optimisticLock == null) {
            optimisticLock = new OptimisticLock(3);
        }
        if (outbox == null) {
            outbox = new Outbox(500, 100);
        }
    }

    public record OptimisticLock(int maxRetries) {
        public OptimisticLock {
            if (maxRetries <= 0) {
                maxRetries = 3;
            }
        }
    }

    public record Outbox(long relayFixedDelayMs, int batchSize) {
        public Outbox {
            if (relayFixedDelayMs <= 0) {
                relayFixedDelayMs = 500;
            }
            if (batchSize <= 0) {
                batchSize = 100;
            }
        }
    }
}
