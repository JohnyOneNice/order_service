package com.example.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
    private UUID orderId;
    private UUID userId;
    private String productId;
    private int productCount;
    private Long price;
    private String status;
    private String idempotencyKey;
    private Long deliverySlotId;
    private java.time.LocalDate deliveryDate;
} 