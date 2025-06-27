package com.example.order_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;

@DynamicUpdate
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
    private String productId;
    private int productCount;
    private String idempotencyKey;
    private Long deliverySlotId;
    private java.time.LocalDate deliveryDate;

}