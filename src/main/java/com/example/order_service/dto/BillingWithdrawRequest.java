package com.example.order_service.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class BillingWithdrawRequest {
    private UUID userId;
    private Long amount;
}