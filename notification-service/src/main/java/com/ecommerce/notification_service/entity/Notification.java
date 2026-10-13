package com.ecommerce.notification_service.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_order_type",
                columnNames = {"order_id", "notification_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 100)
    private String sourceEventId;

    @Column(nullable = false)
    private Instant createdAt;

    public static Notification of(Long orderId, NotificationType type, String recipient,
                                  String subject, String sourceEventId) {
        Notification n = new Notification();
        n.orderId = orderId;
        n.type = type;
        n.recipient = recipient;
        n.subject = subject;
        n.sourceEventId = sourceEventId;
        n.createdAt = Instant.now();
        return n;
    }
}