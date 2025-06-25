package com.example.order_service.service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setProductId(request.getProductId());
        order.setProductCount(request.getProductCount());
        order.setPrice(request.getPrice());
        order.setStatus("Pending");
        order = orderRepository.save(order);
        return OrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .price(order.getPrice())
                .productId(order.getProductId())
                .build();
    }
}
