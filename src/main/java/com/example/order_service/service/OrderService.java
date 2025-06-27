package com.example.order_service.service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.service.OrderEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request, String idempotencyKey) {
        // Проверяем, есть ли уже заказ с таким токеном
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(order -> OrderResponse.builder()
                        .orderId(order.getId())
                        .status(order.getStatus())
                        .price(order.getPrice())
                        .productId(order.getProductId())
                        .deliverySlotId(order.getDeliverySlotId())
                        .deliveryDate(order.getDeliveryDate())
                        .build())
                .orElseGet(() -> {
                    Order order = new Order();
                    order.setUserId(request.getUserId());
                    order.setProductId(request.getProductId());
                    order.setProductCount(request.getProductCount());
                    order.setPrice(request.getPrice());
                    order.setStatus("Pending");
                    order.setIdempotencyKey(idempotencyKey);
                    order.setDeliverySlotId(request.getDeliverySlotId());
                    order.setDeliveryDate(request.getDeliveryDate());
                    try {
                        order = orderRepository.save(order);
                    } catch (DataIntegrityViolationException e) {
                        order = orderRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
                    }
                    // Отправка события OrderCreated
                    OrderCreatedEvent event = OrderCreatedEvent.builder()
                            .orderId(order.getId())
                            .userId(order.getUserId())
                            .productId(order.getProductId())
                            .productCount(order.getProductCount())
                            .price(order.getPrice())
                            .status(order.getStatus())
                            .idempotencyKey(order.getIdempotencyKey())
                            .deliverySlotId(order.getDeliverySlotId())
                            .deliveryDate(order.getDeliveryDate())
                            .build();
                    orderEventPublisher.publishOrderCreated(event);
                    return OrderResponse.builder()
                            .orderId(order.getId())
                            .status(order.getStatus())
                            .price(order.getPrice())
                            .productId(order.getProductId())
                            .deliverySlotId(order.getDeliverySlotId())
                            .deliveryDate(order.getDeliveryDate())
                            .build();
                });
    }
}
