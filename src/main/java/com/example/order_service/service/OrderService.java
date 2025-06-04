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
        UUID orderId = null;
        String status = null;
        String message = null;

        try {
            // Резерв товара через Inventory-service
            inventoryClient.reserve(new ProductReserveRequest("product-123", 1)); // можно передавать из запроса

            // Списание средств через billing-service
            billingClient.withdraw(request.getUserId(), request.getPrice());

            // Создаём заказ (сохраняем в БД)
            Order order = Order.builder()
                .userId(request.getUserId())
                .price(request.getPrice())
                .status("PAID")
                .build();

            order = orderRepository.save(order);
            orderId = order.getId();
            status = order.getStatus();

            message = "🎉 Заказ №" + orderId + " успешно оформлен на сумму: " + request.getPrice() + " у.е.";

            // Резервирование курьера (доставка)
            try {
                deliveryClient.reserveDelivery(new DeliveryRequest(
                        orderId,
                        "Курьер #42", // можно заменить на рандом или выбрать из списка
                        LocalDateTime.now().plusHours(2)
                ));
            } catch (Exception delEx) {
                System.err.println("⚠️ Ошибка при резервировании доставки: " + delEx.getMessage());
            }

    } catch (Exception ex) {

        // Биллинг вернул ошибку
        success = false;
        status = "DECLINED";
        message = "❌ Не удалось оформить заказ: недостаточно средств или ошибка оплаты.";
    }

        // Отправка уведомления через notifications-service
        try {
            notificationsClient.sendNotification(new NotificationRequest(
                    request.getUserId(),
                    message
            ));
        } catch (Exception notifyEx) {
            System.err.println("Ошибка при отправке уведомления: " + notifyEx.getMessage());
        }

        return OrderResponse.builder()
                .orderId(orderId)
                .status(status)
                .build();
    }
}