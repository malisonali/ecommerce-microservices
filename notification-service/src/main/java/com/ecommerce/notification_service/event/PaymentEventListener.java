package com.ecommerce.notification_service.event;

import com.ecommerce.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "payment-events",
            properties = "spring.json.value.default.type=com.ecommerce.notification_service.event.PaymentEvent")
    public void onPaymentEvent(PaymentEvent event) {
        log.info("Received {} for order {}", event.eventType(), event.orderId());
        notificationService.handlePaymentEvent(event);
    }
}