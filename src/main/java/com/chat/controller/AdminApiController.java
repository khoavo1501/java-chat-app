package com.chat.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chat.dto.ThemeUpdateRequest;
import com.chat.dto.UserProfileResponse;
import com.chat.service.PresenceService;
import com.chat.service.UserService;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final UserService userService;
    private final PresenceService presenceService;

    public AdminApiController(UserService userService, PresenceService presenceService) {
        this.userService = userService;
        this.presenceService = presenceService;
    }

    @GetMapping("/users")
    public List<UserProfileResponse> listUsers() {
        return userService.listAllProfiles();
    }

    @PatchMapping("/users/{username}/role/{role}")
    public ResponseEntity<Void> updateRole(@PathVariable("username") String username,
            @PathVariable("role") String role,
            @RequestBody(required = false) RoleToggleRequest request) {
        boolean enabled = request == null || request.isEnabled();
        userService.updateRole(username, role, enabled);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/{username}/theme")
    public UserProfileResponse updateTheme(@PathVariable("username") String username,
            @Valid @RequestBody ThemeUpdateRequest request) {
        userService.updateTheme(username, request.getTheme());
        return userService.getProfile(username);
    }

    @PostMapping("/users/{username}/force-offline")
    public ResponseEntity<Void> forceOffline(@PathVariable("username") String username) {
        presenceService.forceOffline(username);
        return ResponseEntity.ok().build();
    }

    public static class RoleToggleRequest {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}