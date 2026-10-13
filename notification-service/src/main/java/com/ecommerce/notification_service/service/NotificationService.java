package com.ecommerce.notification_service.service;

import com.ecommerce.notification_service.entity.Notification;
import com.ecommerce.notification_service.entity.NotificationType;
import com.ecommerce.notification_service.entity.OrderRecipient;
import com.ecommerce.notification_service.event.InventoryEvent;
import com.ecommerce.notification_service.event.OrderEvent;
import com.ecommerce.notification_service.exception.RecipientNotKnownYetException;
import com.ecommerce.notification_service.repository.NotificationRepository;
import com.ecommerce.notification_service.repository.OrderRecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String FROM = "orders@ecommerce.local";

    private final NotificationRepository notificationRepository;
    private final OrderRecipientRepository recipientRepository;
    private final JavaMailSender mailSender;

    @Transactional
    public void handleOrderEvent(OrderEvent event) {
        Long orderId = event.orderId();

        // Every order event carries the email, so keep the address book up to date
        if (event.userEmail() != null && !recipientRepository.existsById(orderId)) {
            recipientRepository.save(new OrderRecipient(orderId, event.userEmail()));
        }

        switch (event.eventType()) {
            case OrderEvent.ORDER_PLACED -> send(orderId, NotificationType.ORDER_RECEIVED,
                    event.userEmail(),
                    "We received your order #" + orderId,
                    "Thanks for your order #" + orderId + ".\n"
                            + "Total: ₹" + event.totalAmount().toPlainString() + "\n\n"
                            + "We're checking stock now and will confirm shortly.",
                    event.eventId());

            case OrderEvent.ORDER_CANCELLED -> {
                // If we already said "sorry, out of stock", a second "cancelled" email is noise
                if (notificationRepository.existsByOrderIdAndType(orderId, NotificationType.ORDER_REJECTED)) {
                    log.info("Order {} was already rejected, skipping the cancel email", orderId);
                    return;
                }
                send(orderId, NotificationType.ORDER_CANCELLED, event.userEmail(),
                        "Your order #" + orderId + " was cancelled",
                        "Your order #" + orderId + " has been cancelled.\n"
                                + "If you paid, the refund will reach you in 5-7 days.",
                        event.eventId());
            }

            default -> log.debug("No email for {} on order {}", event.eventType(), orderId);
        }
    }

    @Transactional
    public void handleInventoryEvent(InventoryEvent event) {
        Long orderId = event.orderId();

        // The storeroom's reply has no email, so look it up in our own address book
        String email = recipientRepository.findById(orderId)
                .map(OrderRecipient::getUserEmail)
                .orElseThrow(() -> new RecipientNotKnownYetException(orderId));

        switch (event.eventType()) {
            case InventoryEvent.STOCK_RESERVED -> send(orderId, NotificationType.ORDER_CONFIRMED, email,
                    "Your order #" + orderId + " is confirmed 🎉",
                    "Good news! Your order #" + orderId + " is confirmed and will ship soon.",
                    event.eventId());

            case InventoryEvent.STOCK_RESERVATION_FAILED -> send(orderId, NotificationType.ORDER_REJECTED, email,
                    "Sorry, we couldn't complete order #" + orderId,
                    "We're sorry. Some items in order #" + orderId + " are out of stock, "
                            + "so the order was cancelled. You have not been charged.",
                    event.eventId());

            default -> log.debug("No email for {} on order {}", event.eventType(), orderId);
        }
    }

    private void send(Long orderId, NotificationType type, String to,
                      String subject, String body, String sourceEventId) {

        if (notificationRepository.existsByOrderIdAndType(orderId, type)) {
            log.info("{} email for order {} already sent, skipping duplicate", type, orderId);
            return;
        }

        // 1. Record it (the unique key on order + type is the safety net)
        notificationRepository.saveAndFlush(
                Notification.of(orderId, type, to, subject, sourceEventId));

        // 2. Send it. If this throws, the row above is rolled back and Kafka retries.
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);

        log.info("Sent {} email for order {} to {}", type, orderId, to);
    }
}