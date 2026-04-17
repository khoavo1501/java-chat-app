package com.chat.entity;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public class UserAccount {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String passwordHash;

    private Boolean active;

    private boolean online;

    private String theme;

    private Set<String> roles = new LinkedHashSet<>();

    private Set<String> friends = new LinkedHashSet<>();

    private Set<String> incomingFriendRequests = new LinkedHashSet<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void touchForCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (this.createdAt == null) {
            this.createdAt = now;
        }

        this.updatedAt = now;
    }

    public void touchForUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isActive() {
        return active == null || Boolean.TRUE.equals(active);
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public Set<String> getRoles() {
        if (roles == null) {
            roles = new LinkedHashSet<>();
        }
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles == null ? new LinkedHashSet<>() : roles;
    }

    public Set<String> getFriends() {
        if (friends == null) {
            friends = new LinkedHashSet<>();
        }
        return friends;
    }

    public void setFriends(Set<String> friends) {
        this.friends = friends == null ? new LinkedHashSet<>() : friends;
    }

    public Set<String> getIncomingFriendRequests() {
        if (incomingFriendRequests == null) {
            incomingFriendRequests = new LinkedHashSet<>();
        }
        return incomingFriendRequests;
    }

    public void setIncomingFriendRequests(Set<String> incomingFriendRequests) {
        this.incomingFriendRequests = incomingFriendRequests == null ? new LinkedHashSet<>() : incomingFriendRequests;
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

    public boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    public void addRole(String role) {
        if (role != null && !role.trim().isEmpty()) {
            getRoles().add(role.trim().toUpperCase());
        }
    }

    public void removeRole(String role) {
        if (role != null) {
            getRoles().remove(role.trim().toUpperCase());
        }
    }

    public void addFriend(String username) {
        if (username != null && !username.trim().isEmpty()) {
            getFriends().add(username.trim());
        }
    }

    public void removeFriend(String username) {
        if (username != null) {
            getFriends().remove(username.trim());
        }
    }

    public void addIncomingFriendRequest(String username) {
        if (username != null && !username.trim().isEmpty()) {
            getIncomingFriendRequests().add(username.trim());
        }
    }

    public void removeIncomingFriendRequest(String username) {
        if (username != null) {
            getIncomingFriendRequests().remove(username.trim());
        }
    }
}
