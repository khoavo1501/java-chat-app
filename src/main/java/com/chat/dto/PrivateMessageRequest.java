package com.chat.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class PrivateMessageRequest {

    @NotBlank
    @Size(max = 50)
    private String recipient;

    @NotBlank
    @Size(max = 1000)
    private String content;

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
}
