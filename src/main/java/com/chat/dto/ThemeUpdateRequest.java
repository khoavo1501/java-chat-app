package com.chat.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ThemeUpdateRequest {

    @NotBlank
    @Size(max = 32)
    private String theme;

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
}