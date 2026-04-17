package com.chat.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class GroupCreateRequest {

    @NotBlank
    @Size(max = 80)
    private String name;

    private boolean autoApproveJoin;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAutoApproveJoin() {
        return autoApproveJoin;
    }

    public void setAutoApproveJoin(boolean autoApproveJoin) {
        this.autoApproveJoin = autoApproveJoin;
    }
}