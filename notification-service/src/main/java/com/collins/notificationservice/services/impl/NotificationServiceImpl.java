package com.collins.notificationservice.services.impl;

import com.collins.notificationservice.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void sendNotification(String userId, String title, String message) {
        log.info("....................................");
        log.info("NOTIFICATION SENT");
        log.info("TO USER: {}", userId);
        log.info("TITLE: {}", title);
        log.info("MESSAGE: {}", message);
        log.info("....................................");
    }
}
