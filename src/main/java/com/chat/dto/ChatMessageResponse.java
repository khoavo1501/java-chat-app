package com.chat.dto;

import java.time.LocalDateTime;

public class ChatMessageResponse {

    private String scope;
    private String sender;
    private String recipient;
    private String groupCode;
    private String contentType;
    private String content;
    private String attachmentUrl;
    private String attachmentMimeType;
    private LocalDateTime sentAt;

    public ChatMessageResponse() {
    }

    public ChatMessageResponse(String scope, String sender, String recipient, String groupCode, String contentType,
            String content, String attachmentUrl, String attachmentMimeType, LocalDateTime sentAt) {
        this.scope = scope;
        this.sender = sender;
        this.recipient = recipient;
        this.groupCode = groupCode;
        this.contentType = contentType;
        this.content = content;
        this.attachmentUrl = attachmentUrl;
        this.attachmentMimeType = attachmentMimeType;
        this.sentAt = sentAt;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getAttachmentMimeType() {
        return attachmentMimeType;
    }

    public void setAttachmentMimeType(String attachmentMimeType) {
        this.attachmentMimeType = attachmentMimeType;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
