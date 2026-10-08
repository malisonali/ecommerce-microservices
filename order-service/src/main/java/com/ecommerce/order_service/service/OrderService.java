package com.ecommerce.order_service.service;

import com.ecommerce.order_service.client.ProductClient;
import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderItem;
import com.ecommerce.order_service.entity.OrderStatus;
import com.ecommerce.order_service.event.OrderEventPublisher;
import com.ecommerce.order_service.exception.InsufficientStockException;
import com.ecommerce.order_service.exception.OrderNotCancellableException;
import com.ecommerce.order_service.exception.OrderNotFoundException;
import com.ecommerce.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final OrderEventPublisher orderEventPublisher;

    public OrderResponse placeOrder(String userEmail, OrderRequest request) {

        // 1. Merge duplicate lines: [{5, 2}, {5, 1}] becomes {5: 3}
        Map<Long, Integer> quantities = request.items().stream()
                .collect(Collectors.toMap(
                        OrderItemRequest::productId,
                        OrderItemRequest::quantity,
                        Integer::sum,
                        LinkedHashMap::new));

        Order order = Order.builder()
                .userEmail(userEmail)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        // 2. Ask the Product Service about each product
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();

            ProductResponse product = productClient.getProduct(productId);

            // 3. Check stock
            if (product.stockQuantity() < quantity) {
                throw new InsufficientStockException(product.sku(), quantity, product.stockQuantity());
            }

            // 4. Snapshot the product's details and price at this moment
            BigDecimal lineTotal = product.price().multiply(BigDecimal.valueOf(quantity));

            OrderItem item = OrderItem.builder()
                    .productId(product.id())
                    .productName(product.name())
                    .sku(product.sku())
                    .unitPrice(product.price())
                    .quantity(quantity)
                    .lineTotal(lineTotal)
                    .build();

            order.addItem(item);
            total = total.add(lineTotal);
        }

        // 5. Save the order and its items in one go (cascade)
        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        orderEventPublisher.publishOrderPlaced(saved);

        log.info("Order {} placed by {} with total {}", saved.getId(), userEmail, total);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userEmail) {
        return orderRepository.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(Long orderId, String userEmail) {
        return orderRepository.findByIdAndUserEmail(orderId, userEmail)
                .map(this::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public OrderResponse cancelMyOrder(Long orderId, String userEmail) {
        Order order = orderRepository.findByIdAndUserEmail(orderId, userEmail)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getStatus().isCancellableByCustomer()) {
            throw new OrderNotCancellableException(orderId, order.getStatus());
        }

        order.changeStatus(OrderStatus.CANCELLED);

        log.info("Order {} cancelled by customer {}", orderId, userEmail);
        return toResponse(order);
    }

    @Transactional
    public void confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        switch (order.getStatus()) {
            case PENDING -> {
                order.changeStatus(OrderStatus.CONFIRMED);
                log.info("Order {} confirmed: stock reserved", orderId);
            }
            case CANCELLED -> log.warn(
                    "Stock reserved for order {} but it was already cancelled. "
                            + "Stock is held for nothing until we release it (Day 15)", orderId);
            default -> log.info(
                    "Order {} is already {}, ignoring duplicate STOCK_RESERVED",
                    orderId, order.getStatus());
        }
    }

    @Transactional
    public void rejectOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        switch (order.getStatus()) {
            case PENDING -> {
                order.changeStatus(OrderStatus.CANCELLED);
                log.warn("Order {} cancelled: {}", orderId, reason);
            }
            case CANCELLED -> log.info(
                    "Order {} is already CANCELLED, ignoring duplicate STOCK_RESERVATION_FAILED",
                    orderId);
            default -> log.error(
                    "Order {} is {} but stock reservation failed. This should never happen",
                    orderId, order.getStatus());
        }
    }

    // ---------- Admin ----------

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<Order> orders = (status == null)
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);

        return PageResponse.from(orders.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForAdmin(Long orderId) {
        return orderRepository.findWithItemsById(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus, String adminEmail) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus oldStatus = order.getStatus();
        order.changeStatus(newStatus);

        log.info("Order {} moved from {} to {} by admin {}", orderId, oldStatus, newStatus, adminEmail);
        return toResponse(order);
    }

    private OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getUserEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getSku(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt());
    }
}