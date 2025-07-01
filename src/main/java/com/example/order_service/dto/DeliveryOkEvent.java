package com.example.order_service.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class DeliveryOkEvent {
    private UUID orderId;
    private UUID userId;
} 