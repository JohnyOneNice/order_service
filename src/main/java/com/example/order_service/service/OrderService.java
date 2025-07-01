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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.order_service.dto.DeliveryOkEvent;
import com.example.order_service.dto.NotificationRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${notification.send.topic:notification-send}")
    private String notificationSendTopic;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher, KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.kafkaTemplate = kafkaTemplate;
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
                    // Генерация уникального номера заказа
                    String orderNumber = generateOrderNumber();
                    order.setOrderNumber(orderNumber);
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

    @KafkaListener(topics = "delivery-ok", groupId = "order-service-group")
    public void handleDeliveryOk(String message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            DeliveryOkEvent event = mapper.readValue(message, DeliveryOkEvent.class);
            log.info("Получено событие delivery-ok: {}", event);
            if (event.getOrderId() != null) {
                java.util.UUID orderId = event.getOrderId();
                orderRepository.findById(orderId).ifPresent(order -> {
                    order.setStatus("PAIDED");
                    orderRepository.save(order);
                    log.info("Статус заказа {} обновлён на PAIDED", orderId);
                    // Отправка уведомления
                    NotificationRequest notification = new NotificationRequest();
                    notification.setUserId(order.getUserId());
                    String notificationMessage = String.format(
                        "Ваш заказ №%s успешно оформлен на сумму: %.2f. Дата доставки: %s",
                        order.getOrderNumber(),
                        order.getPrice() / 100.0,
                        order.getDeliveryDate() != null ? order.getDeliveryDate().toString() : "-"
                    );
                    notification.setMessage(notificationMessage);
                    kafkaTemplate.send(notificationSendTopic, notification);
                    log.info("Уведомление отправлено в топик {}: {}", notificationSendTopic, notification);
                });
            }
        } catch (Exception e) {
            log.error("Ошибка обработки события delivery-ok", e);
        }
    }

    // Генерация уникального номера заказа
    private String generateOrderNumber() {
        String time = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = new SecureRandom().nextInt(9000) + 1000;
        return time + rand;
    }
}
