package com.ecommerce.notification_service.repository;

import com.ecommerce.notification_service.entity.Notification;
import com.ecommerce.notification_service.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByOrderIdAndType(Long orderId, NotificationType type);
}
