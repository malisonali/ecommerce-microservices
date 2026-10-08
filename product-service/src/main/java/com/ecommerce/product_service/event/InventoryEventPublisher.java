package com.ecommerce.product_service.event;

import com.ecommerce.product_service.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;

    public void publish(InventoryEvent event) {
        try {
            SendResult<String, InventoryEvent> result = kafkaTemplate
                    .send(KafkaTopicConfig.INVENTORY_EVENTS_TOPIC,
                            String.valueOf(event.orderId()), event)
                    .get(10, TimeUnit.SECONDS);

            log.info("Published {} for order {} to partition {} at offset {}",
                    event.eventType(), event.orderId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing " + event.eventType(), e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not publish " + event.eventType() + " for order " + event.orderId(), e);
        }
    }
}