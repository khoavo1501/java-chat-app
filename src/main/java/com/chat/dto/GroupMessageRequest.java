package com.chat.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class GroupMessageRequest {

    @NotBlank
    @Size(max = 32)
    private String groupCode;

    private String content;

    @Size(max = 20)
    private String contentType;

    @Size(max = 4000)
    private String attachmentUrl;

    @Size(max = 100)
    private String attachmentMimeType;

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
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
}