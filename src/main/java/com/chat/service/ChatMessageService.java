package com.chat.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.chat.dto.ChatMessageResponse;
import com.chat.entity.ChatMessage;
import com.chat.entity.GroupRoom;
import com.chat.entity.UserAccount;
import com.chat.repository.ChatMessageRepository;
import com.chat.repository.GroupRoomRepository;
import com.chat.repository.UserAccountRepository;

@Service
public class ChatMessageService {

    private static final String PRIVATE_SCOPE = "PRIVATE";
    private static final String GROUP_SCOPE = "GROUP";

    private final ChatMessageRepository chatMessageRepository;
    private final UserAccountRepository userAccountRepository;
    private final GroupRoomRepository groupRoomRepository;
    private final UserService userService;

    public ChatMessageService(ChatMessageRepository chatMessageRepository,
            UserAccountRepository userAccountRepository,
            GroupRoomRepository groupRoomRepository,
            UserService userService) {
        this.chatMessageRepository = chatMessageRepository;
        this.userAccountRepository = userAccountRepository;
        this.groupRoomRepository = groupRoomRepository;
        this.userService = userService;
    }

    public ChatMessageResponse savePrivateMessage(String sender, String recipient, String content,
            String contentType, String attachmentUrl, String attachmentMimeType) {

        String cleanSender = normalizeText(sender);
        String cleanRecipient = normalizeText(recipient);
        String cleanContent = normalizeText(content);
        String cleanContentType = normalizeContentType(contentType, attachmentUrl);
        String cleanAttachmentUrl = normalizeOptionalText(attachmentUrl);
        String cleanAttachmentMimeType = normalizeOptionalText(attachmentMimeType);

        if (cleanRecipient.isEmpty()) {
            throw new IllegalArgumentException("Nguoi nhan khong hop le.");
        }

        UserAccount senderUser = userService.requireActiveUser(cleanSender);
        UserAccount recipientUser = userService.requireActiveUser(cleanRecipient);

        if (!senderUser.hasRole("ADMIN") && !userService.isVisibleToRegularUsers(recipientUser.getUsername())) {
            throw new IllegalArgumentException("Nguoi nhan khong ton tai.");
        }

        if (!userAccountRepository.existsByUsername(cleanRecipient)) {
            throw new IllegalArgumentException("Nguoi nhan khong ton tai.");
        }

        validateMessageContent(cleanContent, cleanAttachmentUrl);

        ChatMessage message = new ChatMessage();
        message.setScope(PRIVATE_SCOPE);
        message.setSenderUsername(cleanSender);
        message.setRecipientUsername(cleanRecipient);
        message.setContent(cleanContent);
        message.setContentType(cleanContentType);
        message.setAttachmentUrl(cleanAttachmentUrl);
        message.setAttachmentMimeType(cleanAttachmentMimeType);
        message.markSent();

        ChatMessage saved = chatMessageRepository.save(message);
        return toResponse(saved);
    }

    public ChatMessageResponse saveGroupMessage(String sender, String groupCode, String content,
            String contentType, String attachmentUrl, String attachmentMimeType) {

        String cleanSender = normalizeText(sender);
        String cleanGroupCode = normalizeText(groupCode).toUpperCase();
        String cleanContent = normalizeText(content);
        String cleanContentType = normalizeContentType(contentType, attachmentUrl);
        String cleanAttachmentUrl = normalizeOptionalText(attachmentUrl);
        String cleanAttachmentMimeType = normalizeOptionalText(attachmentMimeType);

        if (cleanGroupCode.isEmpty()) {
            throw new IllegalArgumentException("Ma nhom khong hop le.");
        }

        GroupRoom groupRoom = groupRoomRepository.findByGroupCode(cleanGroupCode)
                .orElseThrow(() -> new IllegalArgumentException("Nhom khong ton tai."));

        if (!groupRoom.getMemberUsernames().contains(cleanSender)) {
            throw new IllegalArgumentException("Ban chua tham gia nhom nay.");
        }

        validateMessageContent(cleanContent, cleanAttachmentUrl);

        ChatMessage message = new ChatMessage();
        message.setScope(GROUP_SCOPE);
        message.setSenderUsername(cleanSender);
        message.setGroupCode(groupRoom.getGroupCode());
        message.setContent(cleanContent);
        message.setContentType(cleanContentType);
        message.setAttachmentUrl(cleanAttachmentUrl);
        message.setAttachmentMimeType(cleanAttachmentMimeType);
        message.markSent();

        ChatMessage saved = chatMessageRepository.save(message);
        return toResponse(saved);
    }

    public List<ChatMessageResponse> getConversation(String currentUser, String otherUser) {
        String cleanOtherUser = normalizeText(otherUser);
        if (cleanOtherUser.isEmpty()) {
            throw new IllegalArgumentException("Nguoi dung khong hop le.");
        }

        return chatMessageRepository.findConversation(normalizeText(currentUser), cleanOtherUser)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ChatMessageResponse> getGroupConversation(String groupCode) {
        String cleanGroupCode = normalizeText(groupCode).toUpperCase();
        if (cleanGroupCode.isEmpty()) {
            throw new IllegalArgumentException("Ma nhom khong hop le.");
        }

        return chatMessageRepository.findGroupMessages(cleanGroupCode)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getScope(),
                message.getSenderUsername(),
                message.getRecipientUsername(),
                message.getGroupCode(),
                message.getContentType(),
                message.getContent(),
                message.getAttachmentUrl(),
                message.getAttachmentMimeType(),
                message.getSentAt());
    }

    private void validateMessageContent(String content, String attachmentUrl) {
        if ((content == null || content.trim().isEmpty())
                && (attachmentUrl == null || attachmentUrl.trim().isEmpty())) {
            throw new IllegalArgumentException("Noi dung hoac anh/icon phai duoc cung cap.");
        }

        if (content != null && content.trim().length() > 1000) {
            throw new IllegalArgumentException("Noi dung tin nhan toi da 1000 ky tu.");
        }
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptionalText(String value) {
        String cleaned = normalizeText(value);
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String normalizeContentType(String contentType, String attachmentUrl) {
        String cleaned = normalizeText(contentType).toUpperCase();
        if (!cleaned.isEmpty()) {
            return cleaned;
        }

        return attachmentUrl == null || attachmentUrl.trim().isEmpty() ? "TEXT" : "IMAGE";
    }
}