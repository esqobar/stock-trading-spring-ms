package com.collins.notificationservice.services;

public interface NotificationService {
    void sendNotification(String userId, String title, String message);
}
