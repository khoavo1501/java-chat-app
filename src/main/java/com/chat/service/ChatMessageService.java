package com.chat.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.dto.ChatMessageResponse;
import com.chat.entity.ChatMessage;
import com.chat.repository.ChatMessageRepository;
import com.chat.repository.UserAccountRepository;

@Service
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserAccountRepository userAccountRepository;

    public ChatMessageService(ChatMessageRepository chatMessageRepository,
            UserAccountRepository userAccountRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public ChatMessageResponse savePrivateMessage(String sender, String recipient, String content) {
        String cleanRecipient = recipient == null ? "" : recipient.trim();
        String cleanContent = content == null ? "" : content.trim();

        if (cleanRecipient.isEmpty()) {
            throw new IllegalArgumentException("Nguoi nhan khong hop le.");
        }

        if (!userAccountRepository.existsByUsername(cleanRecipient)) {
            throw new IllegalArgumentException("Nguoi nhan khong ton tai.");
        }

        if (cleanContent.isEmpty()) {
            throw new IllegalArgumentException("Noi dung tin nhan khong duoc de trong.");
        }

        if (cleanContent.length() > 1000) {
            throw new IllegalArgumentException("Noi dung tin nhan toi da 1000 ky tu.");
        }

        ChatMessage message = new ChatMessage();
        message.setSenderUsername(sender);
        message.setRecipientUsername(cleanRecipient);
        message.setContent(cleanContent);

        ChatMessage saved = chatMessageRepository.save(message);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversation(String currentUser, String otherUser) {
        String cleanOtherUser = otherUser == null ? "" : otherUser.trim();
        if (cleanOtherUser.isEmpty()) {
            throw new IllegalArgumentException("Nguoi dung khong hop le.");
        }

        return chatMessageRepository.findConversation(currentUser, cleanOtherUser)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getSenderUsername(),
                message.getRecipientUsername(),
                message.getContent(),
                message.getSentAt());
    }
}
