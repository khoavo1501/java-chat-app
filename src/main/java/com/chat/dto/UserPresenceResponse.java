package com.chat.dto;

public class UserPresenceResponse {

    private String username;
    private boolean online;
    private String theme;

    public UserPresenceResponse() {
    }

    public UserPresenceResponse(String username, boolean online, String theme) {
        this.username = username;
        this.online = online;
        this.theme = theme;
    }

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
}
