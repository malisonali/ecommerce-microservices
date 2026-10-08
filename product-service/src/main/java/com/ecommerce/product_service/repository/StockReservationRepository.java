package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    boolean existsByOrderId(Long orderId);

    Optional<StockReservation> findByOrderId(Long orderId);
}