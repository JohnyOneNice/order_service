package com.example.order_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "MyOrders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)

    private UUID id;
    private UUID userId;
    private Long price; // цена в копейках
    private String status; // CREATED, DECLINED, PROCESSED и т.д.
}