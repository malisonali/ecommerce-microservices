package com.ecommerce.order_service.event;

import com.ecommerce.order_service.config.KafkaTopicConfig;
import com.ecommerce.order_service.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderPlaced(Order order) {
        OrderPlacedEvent event = OrderPlacedEvent.from(order);
        send(order.getId(), event.eventType(), event);
    }

    public void publishOrderCancelled(Order order, String cancelledBy) {
        // Build the event now, while the order and its items are loaded
        OrderCancelledEvent event = OrderCancelledEvent.from(order, cancelledBy);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // Inside a transaction: send only after it commits
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event.orderId(), event.eventType(), event);
                }
            });
        } else {
            send(event.orderId(), event.eventType(), event);
        }
    }

    private void send(Long orderId, String eventType, Object event) {
        String key = String.valueOf(orderId);

        try {
            kafkaTemplate.send(KafkaTopicConfig.ORDER_EVENT_TOPIC, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} for order {}", eventType, orderId, ex);
                        } else {
                            RecordMetadata meta = result.getRecordMetadata();
                            log.info("Published {} for order {} to partition {} at offset {}",
                                    eventType, orderId, meta.partition(), meta.offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Could not send {} for order {}", eventType, orderId, e);
        }
    }
}