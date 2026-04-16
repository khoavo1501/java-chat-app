package com.chat.dto;

import java.time.LocalDateTime;

public class ChatMessageResponse {

    private String sender;
    private String recipient;
    private String content;
    private LocalDateTime sentAt;

    public ChatMessageResponse() {
    }

    public ChatMessageResponse(String sender, String recipient, String content, LocalDateTime sentAt) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.sentAt = sentAt;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
