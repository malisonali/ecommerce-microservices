package com.ecommerce.product_service.service;

import com.ecommerce.product_service.entity.ReservationStatus;
import com.ecommerce.product_service.entity.StockReservation;
import com.ecommerce.product_service.event.OrderPlacedEvent;
import com.ecommerce.product_service.exception.InsufficientStockException;
import com.ecommerce.product_service.repository.ProductRepository;
import com.ecommerce.product_service.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockReservationRepository reservationRepository;

    /**
     * @return true if stock was reserved now, false if this order was already handled
     */
    @Transactional
    public boolean reserveStock(OrderPlacedEvent event) {
        // 1. Already handled this order? Then do nothing.
        if (reservationRepository.existsByOrderId(event.orderId())) {
            log.info("Order {} already handled, skipping duplicate event {}",
                    event.orderId(), event.eventId());
            return false;
        }

        // 2. Reduce stock for every item, locking products in id order
        List<OrderPlacedEvent.Item> items = event.items().stream()
                .sorted(Comparator.comparing(OrderPlacedEvent.Item::productId))
                .toList();

        for (OrderPlacedEvent.Item item : items) {
            int updated = productRepository.decreaseStock(item.productId(), item.quantity());
            if (updated == 0) {
                throw new InsufficientStockException(item.productId(), item.quantity());
            }
        }

        // 3. Remember this order, in the same transaction as the stock change
        reservationRepository.save(StockReservation.builder()
                .orderId(event.orderId())
                .eventId(event.eventId())
                .status(ReservationStatus.RESERVED)
                .build());

        log.info("Reserved stock for order {} ({} product(s))", event.orderId(), items.size());
        return true;
    }

    /**
     * @return true if the rejection was recorded now, false if this order was already handled
     */
    @Transactional
    public boolean recordRejection(OrderPlacedEvent event, String reason) {
        if (reservationRepository.existsByOrderId(event.orderId())) {
            return false;
        }

        reservationRepository.save(StockReservation.builder()
                .orderId(event.orderId())
                .eventId(event.eventId())
                .status(ReservationStatus.REJECTED)
                .reason(reason)
                .build());
        return true;
    }

    @Transactional(readOnly = true)
    public StockReservation getReservation(Long orderId) {
        return reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "No reservation recorded for order " + orderId));
    }

    @Transactional
    public void releaseStock(OrderPlacedEvent event) {
        Optional<StockReservation> found = reservationRepository.findForUpdateByOrderId(event.orderId());

        // The cancel arrived before the order was handled: leave a marker
        if (found.isEmpty()) {
            reservationRepository.save(StockReservation.builder()
                    .orderId(event.orderId())
                    .eventId(event.eventId())
                    .status(ReservationStatus.CANCELLED)
                    .reason("Order cancelled before stock was reserved")
                    .build());
            log.warn("Order {} was cancelled before it was handled. Saved a CANCELLED marker", event.orderId());
            return;
        }

        StockReservation reservation = found.get();

        switch (reservation.getStatus()) {
            case RESERVED -> {
                List<OrderPlacedEvent.Item> items = event.items().stream()
                        .sorted(Comparator.comparing(OrderPlacedEvent.Item::productId))
                        .toList();

                for (OrderPlacedEvent.Item item : items) {
                    int updated = productRepository.increaseStock(item.productId(), item.quantity());
                    if (updated == 0) {
                        log.error("Product {} no longer exists, could not return {} unit(s) for order {}",
                                item.productId(), item.quantity(), event.orderId());
                    }
                }

                reservation.release();
                reservationRepository.save(reservation);
                log.info("Released stock for order {} ({} product(s))", event.orderId(), items.size());
            }
            case RELEASED -> log.info(
                    "Stock for order {} already released, skipping duplicate cancel", event.orderId());
            case REJECTED -> log.info(
                    "Order {} never reserved stock, nothing to release", event.orderId());
            case CANCELLED -> log.info(
                    "Order {} already marked cancelled, skipping duplicate cancel", event.orderId());
        }
    }
}