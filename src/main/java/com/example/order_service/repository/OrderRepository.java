package com.example.order_service.repository;

import com.example.order_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}