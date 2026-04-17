package com.chat.dto;

public class AdminStatsResponse {

    private long totalUsers;
    private long onlineUsers;
    private long adminUsers;
    private long totalGroups;
    private long pendingGroupJoinRequests;
    private long pendingFriendRequests;
    private long totalMessages;

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getOnlineUsers() {
        return onlineUsers;
    }

    public void setOnlineUsers(long onlineUsers) {
        this.onlineUsers = onlineUsers;
    }

    public long getAdminUsers() {
        return adminUsers;
    }

    public void setAdminUsers(long adminUsers) {
        this.adminUsers = adminUsers;
    }

    public long getTotalGroups() {
        return totalGroups;
    }

    public void setTotalGroups(long totalGroups) {
        this.totalGroups = totalGroups;
    }

    public long getPendingGroupJoinRequests() {
        return pendingGroupJoinRequests;
    }

    public void setPendingGroupJoinRequests(long pendingGroupJoinRequests) {
        this.pendingGroupJoinRequests = pendingGroupJoinRequests;
    }

    public long getPendingFriendRequests() {
        return pendingFriendRequests;
    }

    public void setPendingFriendRequests(long pendingFriendRequests) {
        this.pendingFriendRequests = pendingFriendRequests;
    }

    public long getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(long totalMessages) {
        this.totalMessages = totalMessages;
    }
}
