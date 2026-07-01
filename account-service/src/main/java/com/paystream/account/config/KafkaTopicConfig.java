package com.paystream.account.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the PayStream Kafka topics with their partition counts (see design.md).
 * Declaring them as beans makes topic creation explicit rather than relying on
 * broker auto-creation defaults.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String PAYMENT_EVENTS = "payment.events";
    public static final String FRAUD_ALERTS = "fraud.alerts";
    public static final String NOTIFICATION_EVENTS = "notification.events";

    @Bean
    public NewTopic paymentEvents() {
        return TopicBuilder.name(PAYMENT_EVENTS).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic fraudAlerts() {
        return TopicBuilder.name(FRAUD_ALERTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic notificationEvents() {
        return TopicBuilder.name(NOTIFICATION_EVENTS).partitions(3).replicas(1).build();
    }
}
