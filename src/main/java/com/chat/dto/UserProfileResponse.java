package com.chat.dto;

import java.util.ArrayList;
import java.util.List;

public class UserProfileResponse {

    private String username;
    private boolean online;
    private String theme;
    private List<String> roles = new ArrayList<>();
    private List<String> friends = new ArrayList<>();
    private List<String> incomingFriendRequests = new ArrayList<>();

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles == null ? new ArrayList<>() : roles;
    }

    public List<String> getFriends() {
        return friends;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends == null ? new ArrayList<>() : friends;
    }

    public List<String> getIncomingFriendRequests() {
        return incomingFriendRequests;
    }

    public void setIncomingFriendRequests(List<String> incomingFriendRequests) {
        this.incomingFriendRequests = incomingFriendRequests == null ? new ArrayList<>() : incomingFriendRequests;
    }
}