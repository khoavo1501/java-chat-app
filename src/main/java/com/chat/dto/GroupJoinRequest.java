package com.chat.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class GroupJoinRequest {

    @NotBlank
    @Size(max = 32)
    private String groupCode;

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }
}