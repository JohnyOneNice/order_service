package com.example.order_service.client;

import com.example.order_service.dto.ProductReserveRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", url = "${inventory-service.url}")
public interface InventoryClient {

    @PostMapping("/api/inventory/reserve")
    void reserve(@RequestBody ProductReserveRequest request);
}