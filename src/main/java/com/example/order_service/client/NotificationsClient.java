package com.example.order_service.client;

import com.example.order_service.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notifications-service", url = "${notification-service.url}")
public interface NotificationsClient {

    @PostMapping("/api/notifications")
    void sendNotification(@RequestBody NotificationRequest request);
}