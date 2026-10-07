package com.ecommerce.product_service.service;

import com.ecommerce.product_service.event.OrderPlacedEvent;
import com.ecommerce.product_service.exception.InsufficientStockException;
import com.ecommerce.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {
    private final ProductRepository productRepository;

    @Transactional
    public void reserveStock(OrderPlacedEvent event) {
        // Always lock products in the same order (by id) to avoid deadlocks
        List<OrderPlacedEvent.Item> items = event.items().stream()
                .sorted(Comparator.comparing(OrderPlacedEvent.Item::productId))
                .toList();

        for (OrderPlacedEvent.Item item : items) {
            int updated = productRepository.decreaseStock(item.productId(), item.quantity());
            if (updated == 0) {
                // Throwing rolls back every decrease already made for this order
                throw new InsufficientStockException(item.productId(), item.quantity());
            }
        }

        log.info("Reserved stock for order {} ({} product(s))", event.orderId(), items.size());
    }
}
