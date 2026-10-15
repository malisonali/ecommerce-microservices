package com.ecommerce.payment_service.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    private Long orderId;

    @Column(nullable = false, length = 100)
    private String userEmail;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 64)
    private String transactionId;

    @Column(length = 255)
    private String failureReason;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static Payment pending(Long orderId, String userEmail, BigDecimal amount) {
        Payment payment = new Payment();
        payment.orderId = orderId;
        payment.userEmail = userEmail;
        payment.amount = amount;
        payment.status = PaymentStatus.PENDING;
        return payment;
    }

    public static Payment cancelledBeforeKnown(Long orderId, String userEmail) {
        Payment payment = pending(orderId, userEmail, BigDecimal.ZERO);
        payment.status = PaymentStatus.CANCELLED;
        return payment;
    }

    public void complete(String transactionId) {
        requirePending();
        this.status = PaymentStatus.COMPLETED;
        this.transactionId = transactionId;
    }

    public void fail(String reason) {
        requirePending();
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void cancel() {
        requirePending();
        this.status = PaymentStatus.CANCELLED;
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    private void requirePending() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment for order " + orderId + " is " + status + ", not PENDING");
        }
    }
}