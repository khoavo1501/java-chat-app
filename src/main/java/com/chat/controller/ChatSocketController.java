package com.chat.controller;

import java.security.Principal;

import javax.validation.Valid;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.chat.dto.ChatMessageResponse;
import com.chat.dto.GroupMessageRequest;
import com.chat.dto.GroupResponse;
import com.chat.dto.PrivateMessageRequest;
import com.chat.service.ChatMessageService;
import com.chat.service.GroupService;
import com.chat.service.NotificationService;

@Controller
public class ChatSocketController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationService notificationService;
    private final GroupService groupService;

    public ChatSocketController(ChatMessageService chatMessageService,
            SimpMessagingTemplate simpMessagingTemplate,
            NotificationService notificationService,
            GroupService groupService) {
        this.chatMessageService = chatMessageService;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.notificationService = notificationService;
        this.groupService = groupService;
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
                    request.getContent(),
                    request.getContentType(),
                    request.getAttachmentUrl(),
                    request.getAttachmentMimeType());

            simpMessagingTemplate.convertAndSendToUser(saved.getRecipient(), "/queue/messages", saved);
            simpMessagingTemplate.convertAndSendToUser(saved.getSender(), "/queue/messages", saved);

            if (!saved.getSender().equals(saved.getRecipient())) {
                notificationService.sendToUser(
                        saved.getRecipient(),
                        "PRIVATE_MESSAGE",
                        "Tin nhan moi",
                        saved.getSender() + " vua gui tin nhan cho ban.",
                        saved.getSender(),
                        null);
            }
        } catch (IllegalArgumentException ex) {
            simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", ex.getMessage());
        }
    }

    @MessageMapping("/chat.group")
    public void handleGroupMessage(@Valid @Payload GroupMessageRequest request, Principal principal) {
        if (principal == null) {
            return;
        }

        try {
            ChatMessageResponse saved = chatMessageService.saveGroupMessage(
                    principal.getName(),
                    request.getGroupCode(),
                    request.getContent(),
                    request.getContentType(),
                    request.getAttachmentUrl(),
                    request.getAttachmentMimeType());

            simpMessagingTemplate.convertAndSend("/topic/groups/" + saved.getGroupCode(), saved);

            GroupResponse group = groupService.getGroup(saved.getGroupCode());
            notificationService.sendToUsers(
                    group.getMemberUsernames(),
                    "GROUP_MESSAGE",
                    "Tin nhan nhom moi",
                    saved.getSender() + " vua nhan trong nhom " + group.getName() + ".",
                    saved.getSender(),
                    saved.getGroupCode(),
                    saved.getSender());
        } catch (IllegalArgumentException ex) {
            simpMessagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", ex.getMessage());
        }
    }
}
