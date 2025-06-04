package com.example.order_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReserveRequest {
    private String productCode;
    private int quantity;
}