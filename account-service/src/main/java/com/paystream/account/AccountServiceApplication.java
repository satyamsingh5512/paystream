package com.paystream.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PayStream Account Service (port 8082).
 *
 * <p>Implements CQRS: a strongly-consistent write model (optimistic locking) plus an
 * eventually-consistent read model fed by Kafka. State changes and their events are
 * persisted atomically via the Transactional Outbox and relayed on a schedule.
 */
@EnableScheduling
@SpringBootApplication
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
