package com.example.order_service.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private UUID orderId;
    private String status;
}