package com.example.order_service.service;


import com.example.order_service.client.*;
import com.example.order_service.dto.*;
import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final BillingClient billingClient;
    private final InventoryClient inventoryClient;
    private final DeliveryClient deliveryClient;
    private final NotificationsClient notificationsClient;
    private final OrderRepository orderRepository;

    public OrderResponse createOrder(OrderRequest request) {
        boolean billingDone = false;
        boolean inventoryDone = false;
        boolean deliveryDone = false;

        String status = "DECLINED";
        String message;

        UUID orderId = null;

        try {
            // Шаг 1. Списание средств
            billingClient.withdraw(request.getUserId(), request.getPrice());
            billingDone = true;
            log.info("✅ Списаны средства пользователя {}", request.getUserId());

            // Шаг 2. Резерв товара
            inventoryClient.reserve(new ProductReserveRequest(
                    request.getProductCode(),
                    1
            ));
            inventoryDone = true;
            log.info("✅ Забронирован товар '{}'", request.getProductCode());

            // Шаг 3. Создание заказа (до бронирования курьера! чтобы получить orderId)
            Order order = Order.builder()
                    .userId(request.getUserId())
                    .price(request.getPrice())
                    .status("PAID")
                    .build();

            Order savedOrder = orderRepository.save(order);
            orderId = savedOrder.getId();

            log.info("✅ Заказ сохранён в базе: {}", orderId);

            // Шаг 4. Резерв курьера
            deliveryClient.reserveDelivery(new DeliveryRequest(
                    orderId,
                    "Курьер #1",
                    LocalDateTime.now().plusHours(2)
            ));
            deliveryDone = true;
            log.info("✅ Курьер зарезервирован для заказа: {}", orderId);

            // Уведомление об успехе
            message = "🎉 Заказ №" + orderId + " успешно оформлен!";
            status = "PAID";
            sendNotification(request.getUserId(), message);

        } catch (Exception e) {
            log.error("❌ Ошибка при создании заказа: {}", e.getMessage());

            // Компенсация
            if (deliveryDone) {
                try {
                    deliveryClient.cancelDelivery(new DeliveryCancelRequest(orderId));
                    log.info("↩️ Отмена бронирования курьера для заказа {}", orderId);
                } catch (Exception ex) {
                    log.error("⚠️ Ошибка при отмене доставки: {}", ex.getMessage());
                }
            }

            if (inventoryDone) {
                try {
                    inventoryClient.release(new ProductReleaseRequest(
                            request.getProductCode(), 1
                    ));
                    log.info("↩️ Возврат товара '{}'", request.getProductCode());
                } catch (Exception ex) {
                    log.error("⚠️ Ошибка при откате инвентаря: {}", ex.getMessage());
                }
            }

            if (billingDone) {
                try {
                    billingClient.refund(request.getUserId(), request.getPrice());
                    log.info("↩️ Возврат средств пользователю {}", request.getUserId());
                } catch (Exception ex) {
                    log.error("⚠️ Ошибка при возврате средств: {}", ex.getMessage());
                }
            }

            message = "❌ Заказ не оформлен: " + e.getMessage();
            sendNotification(request.getUserId(), message);
        }

        return OrderResponse.builder()
                .orderId(orderId)
                .status(status)
                .build();
    }

    private void sendNotification(UUID userId, String message) {
        try {
            notificationsClient.sendNotification(
                    new NotificationRequest(userId, message));
            log.info("📬 Отправлено уведомление пользователю {}: {}", userId, message);
        } catch (Exception e) {
            log.error("⚠️ Ошибка при отправке уведомления: {}", e.getMessage());
        }
    }
}