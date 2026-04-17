package com.chat.dto;

public class GroupAutoApproveRequest {

    private boolean autoApproveJoin;

    public boolean isAutoApproveJoin() {
        return autoApproveJoin;
    }

    public void setAutoApproveJoin(boolean autoApproveJoin) {
        this.autoApproveJoin = autoApproveJoin;
    }
}