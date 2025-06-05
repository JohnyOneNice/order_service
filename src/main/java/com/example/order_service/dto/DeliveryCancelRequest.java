package com.example.order_service.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryCancelRequest {
    private UUID orderId;
}