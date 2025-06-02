package com.example.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "billing-service", url = "${billing-service.url}")
public interface BillingClient {

    @PostMapping("/api/billing/withdraw/{userId}")
    void withdraw(@PathVariable("userId") UUID userId, @RequestParam("amount") Long amount);
}