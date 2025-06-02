package com.example.order_service.service;

import com.example.order_service.client.BillingClient;
import com.example.order_service.dto.BillingWithdrawRequest;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final BillingClient billingClient;
    private final OrderRepository orderRepository;

    public OrderResponse createOrder(OrderRequest request) {
        try {
            // 1. Попытка снять средства
            billingClient.withdraw(request.getUserId(), request.getPrice());
        } catch (Exception ex) {
            // Ошибка при снятии денег, заказ не создаётся
            return OrderResponse.builder()
                    .orderId(null)
                    .status("DECLINED: " + ex.getMessage())
                    .build();
        }

        // 2. Создаём заказ в БД
        Order order = Order.builder()
                .userId(request.getUserId())
                .price(request.getPrice())
                .status("PAID")
                .build();

        order = orderRepository.save(order);

        return OrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .build();
    }
}