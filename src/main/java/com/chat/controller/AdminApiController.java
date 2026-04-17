package com.chat.controller;

import java.security.Principal;
import java.util.List;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chat.dto.AdminStatsResponse;
import com.chat.dto.GroupAutoApproveRequest;
import com.chat.dto.GroupResponse;
import com.chat.dto.ThemeUpdateRequest;
import com.chat.dto.UserProfileResponse;
import com.chat.repository.ChatMessageRepository;
import com.chat.service.GroupService;
import com.chat.service.PresenceService;
import com.chat.service.UserService;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final UserService userService;
    private final PresenceService presenceService;
    private final GroupService groupService;
    private final ChatMessageRepository chatMessageRepository;

    public AdminApiController(UserService userService,
            PresenceService presenceService,
            GroupService groupService,
            ChatMessageRepository chatMessageRepository) {
        this.userService = userService;
        this.presenceService = presenceService;
        this.groupService = groupService;
        this.chatMessageRepository = chatMessageRepository;
    }

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        AdminStatsResponse response = new AdminStatsResponse();
        response.setTotalUsers(userService.countAllUsers());
        response.setOnlineUsers(userService.countOnlineUsers());
        response.setAdminUsers(userService.countAdminUsers());
        response.setTotalGroups(groupService.countAllGroups());
        response.setPendingGroupJoinRequests(groupService.countPendingJoinRequests());
        response.setPendingFriendRequests(userService.countPendingFriendRequests());
        response.setTotalMessages(chatMessageRepository.count());
        return response;
    }

    @GetMapping("/users")
    public List<UserProfileResponse> listUsers() {
        return userService.listAllProfiles();
    }

    @GetMapping("/groups")
    public List<GroupResponse> listGroups() {
        return groupService.listAllGroups();
    }

    @PatchMapping("/groups/{groupCode}/auto-approve")
    public GroupResponse toggleGroupAutoApprove(@PathVariable("groupCode") String groupCode,
            @Valid @RequestBody GroupAutoApproveRequest request) {
        return groupService.toggleAutoApproveAsAdmin(groupCode, request.isAutoApproveJoin());
    }

    @DeleteMapping("/groups/{groupCode}")
    public ResponseEntity<Void> deleteGroup(@PathVariable("groupCode") String groupCode) {
        groupService.deleteGroupAsAdmin(groupCode);
        return ResponseEntity.noContent().build();
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

    @PatchMapping("/users/{username}/status")
    public UserProfileResponse updateAccountStatus(@PathVariable("username") String username,
            @RequestBody(required = false) AccountStatusRequest request,
            Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Nguoi dung chua dang nhap.");
        }

        boolean active = request == null || request.isActive();
        UserProfileResponse response = userService.updateAccountStatus(principal.getName(), username, active);

        if (!active) {
            presenceService.forceOffline(username);
        }

        return response;
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

    public static class AccountStatusRequest {
        private boolean active = true;

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}