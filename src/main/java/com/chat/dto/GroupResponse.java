package com.chat.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GroupResponse {

    private String groupCode;
    private String name;
    private String ownerUsername;
    private List<String> memberUsernames = new ArrayList<>();
    private List<String> pendingUsernames = new ArrayList<>();
    private boolean autoApproveJoin;
    private LocalDateTime createdAt;

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public List<String> getMemberUsernames() {
        return memberUsernames;
    }

    public void setMemberUsernames(List<String> memberUsernames) {
        this.memberUsernames = memberUsernames == null ? new ArrayList<>() : memberUsernames;
    }

    public List<String> getPendingUsernames() {
        return pendingUsernames;
    }

    public void setPendingUsernames(List<String> pendingUsernames) {
        this.pendingUsernames = pendingUsernames == null ? new ArrayList<>() : pendingUsernames;
    }

    public boolean isAutoApproveJoin() {
        return autoApproveJoin;
    }

    public void setAutoApproveJoin(boolean autoApproveJoin) {
        this.autoApproveJoin = autoApproveJoin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}