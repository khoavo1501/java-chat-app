package com.chat.service;

import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.chat.dto.NotificationResponse;

@Service
public class NotificationService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public NotificationService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void sendToUser(String username, String type, String title, String message, String actorUsername,
            String groupCode) {
        String cleanUsername = normalize(username);
        if (cleanUsername.isEmpty()) {
            return;
        }

        NotificationResponse payload = new NotificationResponse();
        payload.setType(normalize(type));
        payload.setTitle(normalize(title));
        payload.setMessage(normalize(message));
        payload.setActorUsername(normalize(actorUsername));
        payload.setGroupCode(normalize(groupCode));
        payload.setCreatedAt(LocalDateTime.now());

        simpMessagingTemplate.convertAndSendToUser(cleanUsername, "/queue/notifications", payload);
    }

    public void sendToUsers(Collection<String> usernames, String type, String title, String message,
            String actorUsername, String groupCode, String excludedUsername) {
        if (usernames == null || usernames.isEmpty()) {
            return;
        }

        String excluded = normalize(excludedUsername);
        for (String username : usernames) {
            String cleanUsername = normalize(username);
            if (cleanUsername.isEmpty() || cleanUsername.equals(excluded)) {
                continue;
            }

            sendToUser(cleanUsername, type, title, message, actorUsername, groupCode);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
