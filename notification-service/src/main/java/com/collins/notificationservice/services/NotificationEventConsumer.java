package com.collins.notificationservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "order.executed")
    public void consumeOrderExecuted(@Payload Map<String, Object> payload) {
        try {
            String userId = (String) payload.get("userId");
            String type = (String) payload.get("type");
            String symbol = (String) payload.get("symbol");
            Object quantity = payload.get("quantity");
            Object price = payload.get("price");
            Object totalAmount = payload.get("totalAmount");

            notificationService.sendNotification(userId, type.equals("BUY")
            ? "ORDER EXECUTED - BUY"
            : "ORDER EXECUTED - SELL",
                    String.format("%s %s shares of %s at %s, Total: %s",
                            type, quantity, symbol, price, totalAmount)
                    );
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "order.failed")
    public void consumeOrderFailed(@Payload Map<String, Object> payload) {
        try {
            String userId = (String) payload.get("userId");
            String symbol = (String) payload.get("symbol");
            String reason = (String) payload.get("reason");

            notificationService.sendNotification(userId, "Order Failed",
                    String.format("Your order for %s failed. Reason: %s",
                            symbol, reason));
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "user.registered")
    public void consumeUserRegistered(@Payload Map<String, Object> payload) {
        try {
            String id = (String) payload.get("id");
            String firstName = (String) payload.get("firstName");
            Object walletBalance = payload.get("walletBalance");

            notificationService.sendNotification(id, "Welcome to Stock Trading Platform",
                    String.format("Welcome %s! %s added to your wallet.",
                            firstName, walletBalance));
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
        }
    }


}
