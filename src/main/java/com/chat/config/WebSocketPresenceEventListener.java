package com.chat.config;

import java.security.Principal;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.chat.service.PresenceService;

@Component
public class WebSocketPresenceEventListener {

    private final PresenceService presenceService;

    public WebSocketPresenceEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        String username = extractUsername(event);
        if (username != null) {
            presenceService.markConnected(username);
        }
    }

    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        String username = extractUsername(event);
        if (username != null) {
            presenceService.markDisconnected(username);
        }
    }

    private String extractUsername(AbstractSubProtocolEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        return principal == null ? null : principal.getName();
    }
}
