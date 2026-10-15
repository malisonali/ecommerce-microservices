package com.ecommerce.payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.time.Duration;

@Configuration
public class KafkaTopicConfig {

    public static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    // This service OWNS payment-events, so it declares it
    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(PAYMENT_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderEventsPaymentDlt() {
        return dlt("order-events" + KafkaErrorHandlingConfig.DLT_SUFFIX);
    }

    @Bean
    public NewTopic inventoryEventsPaymentDlt() {
        return dlt("inventory-events" + KafkaErrorHandlingConfig.DLT_SUFFIX);
    }

    private static NewTopic dlt(String name) {
        return TopicBuilder.name(name)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofDays(30).toMillis()))
                .build();
    }
}