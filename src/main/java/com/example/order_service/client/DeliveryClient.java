package com.example.order_service.client;

import com.example.order_service.dto.DeliveryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "delivery-service", url = "${delivery-service.url}")
public interface DeliveryClient {
    @PostMapping("/api/delivery")
    void reserveDelivery(@RequestBody DeliveryRequest request);
}