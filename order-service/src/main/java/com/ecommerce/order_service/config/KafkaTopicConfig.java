package com.ecommerce.order_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.time.Duration;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_EVENT_TOPIC = "order-events";

    @Bean
    public NewTopic orderEventTopic(){
        return TopicBuilder.name(ORDER_EVENT_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryEventsDlt() {
        return TopicBuilder.name("inventory-events.DLT")
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofDays(30).toMillis()))
                .build();
    }
}
