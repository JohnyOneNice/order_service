package com.example.order_service.dto;

import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequest {
    private UUID userId;
    private String message;
}