package com.chat.controller;

import java.security.Principal;
import java.util.List;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chat.dto.FriendRequestRequest;
import com.chat.dto.ThemeUpdateRequest;
import com.chat.dto.UserProfileResponse;
import com.chat.dto.UserPresenceResponse;
import com.chat.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserPresenceResponse> listUsers() {
        return userService.getAllPresence();
    }

    @GetMapping("/profile")
    public UserProfileResponse profile(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Nguoi dung chua dang nhap.");
        }

        return userService.getProfile(principal.getName());
    }

    @PatchMapping("/theme")
    public UserProfileResponse updateTheme(@Valid @RequestBody ThemeUpdateRequest request, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Nguoi dung chua dang nhap.");
        }

        userService.updateTheme(principal.getName(), request.getTheme());
        return userService.getProfile(principal.getName());
    }

    @PostMapping("/friends/request")
    public ResponseEntity<Void> sendFriendRequest(@Valid @RequestBody FriendRequestRequest request,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        userService.sendFriendRequest(principal.getName(), request.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/friends/accept")
    public ResponseEntity<Void> acceptFriendRequest(@Valid @RequestBody FriendRequestRequest request,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        userService.acceptFriendRequest(principal.getName(), request.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/friends/reject")
    public ResponseEntity<Void> rejectFriendRequest(@Valid @RequestBody FriendRequestRequest request,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        userService.rejectFriendRequest(principal.getName(), request.getUsername());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/friends/{username}")
    public ResponseEntity<Void> removeFriend(@PathVariable("username") String username, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        userService.removeFriend(principal.getName(), username);
        return ResponseEntity.ok().build();
    }
}
