package com.chat.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.chat.dto.UserProfileResponse;
import com.chat.service.UserService;

@Controller
public class ChatPageController {

    private final UserService userService;

    public ChatPageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({ "/", "/chat" })
    public String chatPage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        UserProfileResponse profile = userService.getProfile(principal.getName());
        model.addAttribute("currentUser", profile.getUsername());
        model.addAttribute("currentTheme", profile.getTheme());
        model.addAttribute("isAdmin", profile.getRoles().contains("ADMIN"));
        return "chat";
    }

    @GetMapping("/profile")
    public String profilePage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        UserProfileResponse profile = userService.getProfile(principal.getName());
        model.addAttribute("currentUser", profile.getUsername());
        model.addAttribute("currentTheme", profile.getTheme());
        model.addAttribute("isAdmin", profile.getRoles().contains("ADMIN"));
        return "profile";
    }

    @GetMapping("/groups")
    public String groupsPage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        UserProfileResponse profile = userService.getProfile(principal.getName());
        model.addAttribute("currentUser", profile.getUsername());
        model.addAttribute("currentTheme", profile.getTheme());
        model.addAttribute("isAdmin", profile.getRoles().contains("ADMIN"));
        return "groups";
    }
}
