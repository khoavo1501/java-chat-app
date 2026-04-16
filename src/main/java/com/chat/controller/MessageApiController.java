package com.chat.controller;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chat.dto.ChatMessageResponse;
import com.chat.service.ChatMessageService;

@RestController
@RequestMapping("/api/messages")
public class MessageApiController {

    private final ChatMessageService chatMessageService;

    public MessageApiController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @GetMapping("/{username}")
    public List<ChatMessageResponse> getConversation(
            @PathVariable("username") String username,
            Principal principal) {

        if (principal == null) {
            return Collections.emptyList();
        }

        return chatMessageService.getConversation(principal.getName(), username);
    }
}
