package com.chat.service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.chat.dto.UserPresenceResponse;

@Service
public class PresenceService {

    private final UserService userService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    private final ConcurrentHashMap<String, Integer> activeSessions = new ConcurrentHashMap<>();

    public PresenceService(UserService userService, SimpMessagingTemplate simpMessagingTemplate) {
        this.userService = userService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public synchronized void markConnected(String username) {
        int count = activeSessions.getOrDefault(username, 0) + 1;
        activeSessions.put(username, count);
        userService.updateOnline(username, true);
        broadcastPresence();
    }

    public synchronized void markDisconnected(String username) {
        Integer count = activeSessions.get(username);
        if (count == null) {
            return;
        }

        if (count <= 1) {
            activeSessions.remove(username);
            userService.updateOnline(username, false);
        } else {
            activeSessions.put(username, count - 1);
        }

        broadcastPresence();
    }

    public synchronized void forceOffline(String username) {
        activeSessions.remove(username);
        userService.updateOnline(username, false);
        broadcastPresence();
    }

    public void broadcastPresence() {
        List<UserPresenceResponse> snapshot = userService.getAllPresence();
        simpMessagingTemplate.convertAndSend("/topic/presence", snapshot);
    }
}
