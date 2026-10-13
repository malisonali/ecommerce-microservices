package com.ecommerce.notification_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_recipients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderRecipient {

    @Id
    private Long orderId;

    @Column(nullable = false)
    private String userEmail;

    public OrderRecipient(Long orderId, String userEmail) {
        this.orderId = orderId;
        this.userEmail = userEmail;
    }
}