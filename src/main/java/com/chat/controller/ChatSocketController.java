package com.chat.controller;

import java.security.Principal;

import javax.validation.Valid;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.chat.dto.ChatMessageResponse;
import com.chat.dto.PrivateMessageRequest;
import com.chat.service.ChatMessageService;

@Controller
public class ChatSocketController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public ChatSocketController(ChatMessageService chatMessageService, SimpMessagingTemplate simpMessagingTemplate) {
        this.chatMessageService = chatMessageService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @MessageMapping("/chat.private")
    public void handlePrivateMessage(@Valid @Payload PrivateMessageRequest request, Principal principal) {
        if (principal == null) {
            return;
        }

        try {
            ChatMessageResponse saved = chatMessageService.savePrivateMessage(
                    principal.getName(),
                    request.getRecipient(),
                    request.getContent());

            simpMessagingTemplate.convertAndSendToUser(saved.getRecipient(), "/queue/messages", saved);
            simpMessagingTemplate.convertAndSendToUser(saved.getSender(), "/queue/messages", saved);
        } catch (IllegalArgumentException ex) {
            simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", ex.getMessage());
        }
    }
}
