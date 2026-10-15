package com.ecommerce.notification_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.time.Duration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderEventsNotificationDlt() {
        return dlt("order-events" + KafkaErrorHandlingConfig.DLT_SUFFIX);
    }

    @Bean
    public NewTopic inventoryEventsNotificationDlt() {
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

    @Bean
    public NewTopic paymentEventsNotificationDlt() {
        return dlt("payment-events" + KafkaErrorHandlingConfig.DLT_SUFFIX);
    }
}