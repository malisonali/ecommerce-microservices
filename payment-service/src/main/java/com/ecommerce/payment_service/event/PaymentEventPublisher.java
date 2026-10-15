package com.ecommerce.payment_service.event;

import com.ecommerce.payment_service.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(PaymentEvent event) {
        // join() waits for Kafka to confirm. If the send fails, it throws,
        // the listener fails, and the Day 17 handler retries.
        SendResult<String, Object> result = kafkaTemplate
                .send(KafkaTopicConfig.PAYMENT_EVENTS_TOPIC, String.valueOf(event.orderId()), event)
                .join();

        log.info("Published {} for order {} to partition {} at offset {}",
                event.eventType(), event.orderId(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }
}