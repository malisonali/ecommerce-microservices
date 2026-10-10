package com.ecommerce.order_service.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "outbox_events",
        indexes = @Index(name = "idx_outbox_unpublished", columnList = "published_at, id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(length = 500)
    private String lastError;

    public static OutboxEvent of(String topic, String messageKey, String eventType, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.topic = topic;
        e.messageKey = messageKey;
        e.eventType = eventType;
        e.payload = payload;
        e.createdAt = Instant.now();
        return e;
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
        this.lastError = null;
    }

    public void recordFailure(Throwable error) {
        this.attempts++;
        String msg = error.toString();
        this.lastError = msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}