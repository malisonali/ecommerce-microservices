package com.ecommerce.order_service.event;

import com.ecommerce.order_service.config.KafkaTopicConfig;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.outbox.OutboxEvent;
import com.ecommerce.order_service.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final OutboxEventRepository outboxRepository;
    private final JsonMapper jsonMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishOrderPlaced(Order order) {
        OrderPlacedEvent event = OrderPlacedEvent.from(order);
        saveToOutbox(order.getId(), event.eventType(), event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishOrderCancelled(Order order, String cancelledBy) {
        OrderCancelledEvent event = OrderCancelledEvent.from(order, cancelledBy);
        saveToOutbox(order.getId(), event.eventType(), event);
    }

    private void saveToOutbox(Long orderId, String eventType, Object event) {
        String payload = jsonMapper.writeValueAsString(event);
        outboxRepository.save(OutboxEvent.of(
                KafkaTopicConfig.ORDER_EVENT_TOPIC,
                String.valueOf(orderId),
                eventType,
                payload));
        log.info("Saved {} for order {} to the outbox", eventType, orderId);
    }
}