package com.example.order_service.service;

import com.example.order_service.client.BillingClient;
import com.example.order_service.dto.BillingWithdrawRequest;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.order_service.client.NotificationsClient;
import com.example.order_service.dto.NotificationRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final BillingClient billingClient;
    private final OrderRepository orderRepository;
    private final NotificationsClient notificationsClient;


    public OrderResponse createOrder(OrderRequest request) {
        boolean success = true;
        UUID orderId = null;
        String status = null;
        String message = null;

        try {
            // Пытаемся списать средства через billing-service
            billingClient.withdraw(request.getUserId(), request.getPrice());
//        } catch (Exception ex) {
//            // Ошибка при снятии денег, заказ не создаётся
//            return OrderResponse.builder()
//                    .orderId(null)
//                    .status("DECLINED: " + ex.getMessage())
//                    .build();


        // 2. Создаём заказ в БД
        Order order = Order.builder()
                .userId(request.getUserId())
                .price(request.getPrice())
                .status("PAID")
                .build();

        order = orderRepository.save(order);
        orderId = order.getId();
        status = order.getStatus();

        message = "🎉 Заказ №" + orderId + " успешно оформлен на сумму: " + request.getPrice() + " у.е.";
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