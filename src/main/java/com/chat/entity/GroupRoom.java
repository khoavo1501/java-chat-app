package com.chat.entity;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("groups")
public class GroupRoom {

    @Id
    private String id;

    @Indexed(unique = true)
    private String groupCode;

    private String name;

    private String ownerUsername;

    private Set<String> memberUsernames = new LinkedHashSet<>();

    private Set<String> pendingUsernames = new LinkedHashSet<>();

    private boolean autoApproveJoin;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void touchForCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    public void touchForUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Set<String> getMemberUsernames() {
        if (memberUsernames == null) {
            memberUsernames = new LinkedHashSet<>();
        }
        return memberUsernames;
    }

    public void setMemberUsernames(Set<String> memberUsernames) {
        this.memberUsernames = memberUsernames == null ? new LinkedHashSet<>() : memberUsernames;
    }

    public Set<String> getPendingUsernames() {
        if (pendingUsernames == null) {
            pendingUsernames = new LinkedHashSet<>();
        }
        return pendingUsernames;
    }

    public void setPendingUsernames(Set<String> pendingUsernames) {
        this.pendingUsernames = pendingUsernames == null ? new LinkedHashSet<>() : pendingUsernames;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void addMember(String username) {
        if (username != null && !username.trim().isEmpty()) {
            getMemberUsernames().add(username.trim());
            getPendingUsernames().remove(username.trim());
        }
    }

    public void addPendingMember(String username) {
        if (username != null && !username.trim().isEmpty()) {
            String clean = username.trim();
            if (!getMemberUsernames().contains(clean)) {
                getPendingUsernames().add(clean);
            }
        }
    }

    public void removePendingMember(String username) {
        if (username != null) {
            getPendingUsernames().remove(username.trim());
        }
    }
}