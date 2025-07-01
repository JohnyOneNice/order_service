package com.example.order_service.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class NotificationRequest {
    private UUID userId;
    private String message;
} 