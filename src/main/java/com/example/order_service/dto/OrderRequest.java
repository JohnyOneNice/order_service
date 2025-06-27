package com.example.order_service.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class OrderRequest {
    private UUID userId;
    private Long price;
    private String productId;
    private int productCount;
    private Long deliverySlotId;
    private java.time.LocalDate deliveryDate;
}