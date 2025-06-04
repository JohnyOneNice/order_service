package com.example.order_service.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRequest {
    private UUID orderId;
    private String courierName;
    private LocalDateTime deliverySlot;
}