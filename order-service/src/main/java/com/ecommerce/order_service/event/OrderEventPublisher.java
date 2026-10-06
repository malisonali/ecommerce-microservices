package com.ecommerce.order_service.event;

import com.ecommerce.order_service.config.KafkaTopicConfig;
import com.ecommerce.order_service.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderPlaced(Order order) {
        OrderPlacedEvent event = OrderPlacedEvent.from(order);
        String key = String.valueOf(order.getId());

        try {
            kafkaTemplate.send(KafkaTopicConfig.ORDER_EVENT_TOPIC, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} for order {}",
                                    event.eventType(), order.getId(), ex);
                        } else {
                            RecordMetadata meta = result.getRecordMetadata();
                            log.info("Published {} for order {} to partition {} at offset {}",
                                    event.eventType(), order.getId(), meta.partition(), meta.offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Could not send {} for order {}", event.eventType(), order.getId(), e);
        }
    }
}