package com.ecommerce.notification_service.repository;

import com.ecommerce.notification_service.entity.OrderRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRecipientRepository extends JpaRepository<OrderRecipient, Long> {
}
