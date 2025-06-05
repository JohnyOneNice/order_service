package com.example.order_service.service;

import com.example.order_service.client.BillingClient;
import com.example.order_service.client.NotificationsClient;
import com.example.order_service.client.InventoryClient;
import com.example.order_service.client.DeliveryClient;

import com.example.order_service.dto.*;

import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final BillingClient billingClient;
    private final OrderRepository orderRepository;
    private final NotificationsClient notificationsClient;
    private final InventoryClient inventoryClient;
    private final DeliveryClient deliveryClient;


    public OrderResponse createOrder(OrderRequest request) {
        boolean success = true;
        boolean billingDone = false;
        boolean inventoryDone = false;
        boolean deliveryDone = false;

        UUID orderId = UUID.randomUUID();
        String status = "DECLINED";
        String message;


        try {

            // Шаг 1: списание средств с кошелька пользователя через billing-service
            billingClient.withdraw(request.getUserId(), request.getPrice());
            billingDone = true;
        } catch (Exception e) {
            message = "❌ Недостаточно средств: " + e.getMessage();
            sendNotification(request.getUserId(), message);
            return buildFailedOrderResponse(null, status);
        }

        try {
            // Шаг 2: резервирование товара через Inventory-service
            inventoryClient.reserve(new ProductReserveRequest(request.getProductCode(), 1));
            inventoryDone = true;
        } catch (Exception e) {
            message = "❌ Товар недоступен: " + e.getMessage();
            if (billingDone) billingClient.refund(request.getUserId(), request.getPrice());
            sendNotification(request.getUserId(), message);
            return buildFailedOrderResponse(null, status);
        }

        try {
            // Шаг 3: резервирование доставки (курьер + слот времени)
            deliveryClient.reserveDelivery(new DeliveryRequest(
                    orderId,
                    "Курьер #1",
                    LocalDateTime.now().plusHours(2)
            ));
            deliveryDone = true;
        } catch (Exception e) {
            message = "❌ Нет доступного курьера: " + e.getMessage();
            if (inventoryDone) inventoryClient.release(new ProductReleaseRequest(request.getProductCode(), 1));
            if (billingDone) billingClient.refund(request.getUserId(), request.getPrice());
            sendNotification(request.getUserId(), message);
            return buildFailedOrderResponse(null, status);
        }

            // Шаг 4: Успешный заказ
            Order order = Order.builder()
                    .id(orderId)
                    .userId(request.getUserId())
                    .price(request.getPrice())
                    .status("PAID")
                    .build();
            orderRepository.save(order);

        status = "PAID";
        message = "🎉 Заказ №" + orderId + " успешно оформлен!";
        sendNotification(request.getUserId(), message);

        return OrderResponse.builder()
                .orderId(orderId)
                .status(status)
                .build();
    }

    private void sendNotification(UUID userId, String message) {
        try {
            notificationsClient.sendNotification(
                    new NotificationRequest(userId, message));
        } catch (Exception e) {
            System.err.println("⚠️ Уведомление не отправлено: " + e.getMessage());
        }
    }

    private OrderResponse buildFailedOrderResponse(UUID orderId, String status) {
        return OrderResponse.builder()
                .orderId(orderId)
                .status(status)
                .build();
    }
}