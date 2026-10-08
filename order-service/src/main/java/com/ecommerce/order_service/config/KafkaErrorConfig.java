package com.ecommerce.order_service.config;

import com.ecommerce.order_service.exception.InvalidOrderStatusTransitionException;
import com.ecommerce.order_service.exception.OrderNotFoundException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        // Retry 3 more times, waiting 1 second between tries
        DefaultErrorHandler handler = new DefaultErrorHandler(new FixedBackOff(1000L, 3));

        // These will fail the same way every time, so skip them immediately
        handler.addNotRetryableExceptions(
                OrderNotFoundException.class,
                InvalidOrderStatusTransitionException.class);

        return handler;
    }
}