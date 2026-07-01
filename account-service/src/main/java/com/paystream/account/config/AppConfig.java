package com.paystream.account.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables typed configuration properties and declares OpenAPI metadata. */
@Configuration
@EnableConfigurationProperties(AccountProperties.class)
@OpenAPIDefinition(info = @Info(
        title = "PayStream Account Service API",
        version = "v1",
        description = "CQRS account balances with optimistic locking and Transactional Outbox"))
public class AppConfig {
}
